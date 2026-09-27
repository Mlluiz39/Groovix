package com.seuapp.music.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.seuapp.music.data.api.RetrofitClient
import com.seuapp.music.data.api.YoutubeApi
import com.seuapp.music.data.model.toTrack
import com.seuapp.music.data.repository.PlayerRepository
import com.seuapp.music.data.local.FavoriteEntity
import com.seuapp.music.data.local.MusicDatabase
import com.seuapp.music.data.local.PlaylistEntity
import com.seuapp.music.data.local.RecentTrackEntity
import com.seuapp.music.data.local.UserEntity
import com.seuapp.music.data.match.TrackMatcher
import com.seuapp.music.data.model.FailReport
import com.seuapp.music.data.model.Playlist
import com.seuapp.music.data.model.RemoteConfigResponse
import com.seuapp.music.data.model.Track
import com.seuapp.music.data.spotify.SpotifyCsvParser
import com.seuapp.music.player.MusicService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Quantas buscas de músicas rodamos em paralelo durante a importação. */
private const val SEARCH_PARALLEL = 3

/** Estado da importação de playlist do backup CSV do Spotify. */
data class ImportProgress(
    val current: Int,
    val total: Int,
    val songTitle: String,
    val found: Int,
    val playlistName: String
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    // get() em vez de val: a base URL pode mudar em Configurações (rebuilt do Retrofit)
    private val api get() = RetrofitClient.api
    private val playerRepo = PlayerRepository()
    private val gson = Gson()
    private val db = MusicDatabase.getDatabase(app)
    private val dao = db.musicDao()

    private val prefs = app.getSharedPreferences("groovix_config", Context.MODE_PRIVATE)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _favorites = MutableStateFlow<List<Track>>(emptyList())
    val favorites: StateFlow<List<Track>> = _favorites

    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle

    private val _repeat = MutableStateFlow(false)
    val repeat: StateFlow<Boolean> = _repeat

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

    private val _trending = MutableStateFlow<List<Track>>(emptyList())
    val trending: StateFlow<List<Track>> = _trending

    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage

    /** URL do backend salva (SharedPreferences) — vira ativa na hora que salvar. */
    private val _backendUrl = MutableStateFlow(
        prefs.getString("backend_url", RetrofitClient.DEFAULT_BASE_URL) ?: RetrofitClient.DEFAULT_BASE_URL
    )
    val backendUrl: StateFlow<String> = _backendUrl

    /** Feedback do "Salvar e testar" em Configurações. */
    private val _backendStatus = MutableStateFlow<String?>(null)
    val backendStatus: StateFlow<String?> = _backendStatus

    /** Flags remotas do backend (GET /config) — null enquanto não chegam. */
    private val _remoteConfig = MutableStateFlow<RemoteConfigResponse?>(null)
    val remoteConfig: StateFlow<RemoteConfigResponse?> = _remoteConfig

    /** Resumo da config remota exibido em Configurações. */
    private val _configStatus = MutableStateFlow<String?>(null)
    val configStatus: StateFlow<String?> = _configStatus

    /** Anti-spam da telemetria: 1 relato por faixa/estágio a cada 10 min. */
    private val lastFailSentAt = mutableMapOf<String, Long>()

    private var importJob: Job? = null

    private var queue: List<Track> = emptyList()
    private var queueIndex: Int = -1
    private var shuffleOrder: List<Int> = emptyList()
    private var shufflePos: Int = -1
    private val resolvedUrls = mutableMapOf<String, String>()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingPlay: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                if (_repeat.value) { controller?.seekTo(0); controller?.play() } else { next() }
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            error.printStackTrace()
            _playbackError.value = error.message ?: "Erro de reprodução"
            // (B) telemetria: a stream envelheceu/foi bloqueada no meio do play
            _currentTrack.value?.let {
                reportFail(it, stage = "playback", reason = error.errorCodeName)
            }
        }
    }

    init {
        // Ativa a URL de backend salva ANTES de qualquer chamada de rede
        RetrofitClient.setBaseUrl(_backendUrl.value)
        // (A) puxa as flags de estratégia do backend (limite YT, telemetria...)
        fetchRemoteConfig()

        val token = SessionToken(app, ComponentName(app, MusicService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(playerListener)
            pendingPlay?.let { it(); pendingPlay = null }
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                withContext(Dispatchers.Main) {
                    val c = controller
                    if (c != null) {
                        val dur = c.duration
                        _duration.value = if (dur > 0) dur else 0L
                        _progress.value = if (dur > 0) c.currentPosition.toFloat() / dur else 0f
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try { loadPlaylists() } catch (e: Exception) { e.printStackTrace() }
            try { loadFavorites() } catch (e: Exception) { e.printStackTrace() }
            try { loadRecentTracks() } catch (e: Exception) { e.printStackTrace() }
            try { loadUser() } catch (e: Exception) { e.printStackTrace() }
        }
        viewModelScope.launch {
            try { _trending.value = playerRepo.trending() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun loadUser() {
        val user = dao.getUser()
        if (user != null) {
            _userName.value = user.name
            _userEmail.value = user.email
            _isLoggedIn.value = true
        }
    }

    /**
     * Salva a URL do backend (Configurações), ativa na hora e testa /health.
     * Retorna false se a URL for inválida (aí nada é salvo).
     */
    fun saveAndTestBackend(raw: String): Boolean {
        val normalized = RetrofitClient.normalizeBaseUrl(raw) ?: return false
        prefs.edit().putString("backend_url", normalized).apply()
        RetrofitClient.setBaseUrl(normalized)
        _backendUrl.value = normalized
        _backendStatus.value = "Testando ${normalized}..."
        viewModelScope.launch {
            _backendStatus.value = try {
                val h = api.health()
                if (h.healthy) {
                    // servidor vivo: revalida as flags remotas na hora
                    fetchRemoteConfig()
                    "✅ Backend respondeu — URL salva e ativa"
                } else "⚠️ Respondeu sem 'ok' no /health — URL salva mesmo assim"
            } catch (e: Exception) {
                e.printStackTrace()
                "❌ Sem resposta (${(e.message ?: "erro").take(90)}). URL salva, mas provavelmente está fora do ar."
            }
        }
        return true
    }

    /** Volta pra URL padrão do projeto e testa. */
    fun resetBackendUrl() { saveAndTestBackend(RetrofitClient.DEFAULT_BASE_URL) }

    fun clearBackendStatus() { _backendStatus.value = null }

    /**
     * (A) Config remota: busca GET /config e aplica as flags no app
     * (limite de falhas do YouTube, telemetria ligada...). Silenciosa —
     * backend antigo sem /config ou rede fora => app segue com os defaults.
     */
    private fun fetchRemoteConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cfg = api.config()
                _remoteConfig.value = cfg
                cfg.ytFailLimit?.let { playerRepo.ytFailLimit = it }
                _configStatus.value = buildString {
                    append("⚙️ Config remota v${cfg.version ?: 1}")
                    append(" · limite YouTube=${playerRepo.ytFailLimit}")
                    append(" · stream ${if (cfg.validateStream != false) "validada ✅" else "sem validação"}")
                    append(" · telemetria ${if (cfg.telemetry != false) "✅" else "❌"}")
                }
            } catch (e: Exception) {
                // sem config remota o app continua normal com os defaults
                _remoteConfig.value = null
            }
        }
    }

    /**
     * (B) Telemetria: avisa o backend quando uma faixa falhou, pra a gente
     * ver em logs/failures.jsonl quando o YouTube mudar a estratégia.
     * Fire-and-forget e sem spam (1 relato por faixa/estágio a cada 10 min).
     */
    private fun reportFail(track: Track, stage: String, reason: String) {
        if (_remoteConfig.value?.telemetry == false) return
        val now = System.currentTimeMillis()
        val key = "${track.id}:$stage"
        if (now - (lastFailSentAt[key] ?: 0L) < 10 * 60_000L) return
        if (lastFailSentAt.size > 200) lastFailSentAt.clear()
        lastFailSentAt[key] = now
        val report = FailReport(
            track = track.title.take(300),
            artist = track.channel.take(300),
            source = when {
                track.url.contains("soundcloud", ignoreCase = true) -> "soundcloud"
                track.url.contains("audius", ignoreCase = true) -> "audius"
                else -> "youtube"
            },
            stage = stage,
            reason = reason.take(300)
        )
        viewModelScope.launch(Dispatchers.IO) {
            try { api.reportFail(report) } catch (e: Exception) { /* sem servidor, segue o baile */ }
        }
    }

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertUser(UserEntity(name = name, email = email, password = password))
            _userName.value = name
            _userEmail.value = email
            _isLoggedIn.value = true
        }
    }

    fun updateUser(name: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = dao.getUser()
            dao.insertUser(UserEntity(name = name, email = email, password = user?.password ?: ""))
            _userName.value = name
            _userEmail.value = email
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    fun search() {
        if (_query.value.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _playbackError.value = null
            try {
                // Audius (tocável) + InnerTube (descoberta) — nunca joga exceção pra UI
                val res = playerRepo.search(_query.value)
                _tracks.value = res
                if (res.isEmpty()) {
                    _playbackError.value =
                        "Nada por aqui — servidor acordando ou sem internet. Toque em buscar de novo."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _playbackError.value = "Falha na busca. Verifique a internet e tente de novo."
            } finally { _isLoading.value = false }
        }
    }

    /**
     * Importa o CSV do backup do Spotify: lê as músicas, busca cada uma aqui no app
     * (SoundCloud + Audius + YouTube) e salva numa playlist já tocável.
     * Mostra o progresso em [importProgress] e o resultado em [importMessage].
     */
    fun importSpotifyCsv(uri: Uri, displayName: String? = null) {
        if (importJob?.isActive == true) return
        val playlistName = displayName
            ?.substringBeforeLast('.', displayName)
            ?.trim()
            .orEmpty()
            .ifBlank { "Spotify" }

        importJob = viewModelScope.launch(Dispatchers.IO) {
            _importMessage.value = null
            _importProgress.value = ImportProgress(0, 0, "Lendo o CSV...", 0, playlistName)
            try {
                val csv = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (csv == null) {
                    _importMessage.value = "Não consegui abrir o arquivo."
                    return@launch
                }

                val songs = SpotifyCsvParser.parse(csv)
                if (songs.isEmpty()) {
                    _importMessage.value =
                        "Nenhuma música encontrada no CSV. Confira se é o arquivo de playlist exportado do Spotify."
                    return@launch
                }

                val resolved = ArrayList<Track>(songs.size)
                var found = 0
                var done = 0

                for (batch in songs.chunked(SEARCH_PARALLEL)) {
                    coroutineContext.ensureActive()
                    val matches = batch.map { song ->
                        async {
                            val track = try {
                                TrackMatcher.pickBest(playerRepo.search(song.query), song.title, song.artist)
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { e.printStackTrace(); null }
                            song to track
                        }
                    }.awaitAll()

                    for ((song, track) in matches) {
                        done++
                        if (track != null) { resolved.add(track); found++ }
                        _importProgress.value =
                            ImportProgress(done, songs.size, song.query, found, playlistName)
                    }
                }

                coroutineContext.ensureActive()

                if (resolved.isEmpty()) {
                    _importMessage.value =
                        "Nenhuma música encontrada no Groovix. Verifique a internet e tente de novo."
                    return@launch
                }

                val unique = resolved.distinctBy { it.id }
                dao.insertPlaylist(PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = playlistName,
                    tracksJson = gson.toJson(unique)
                ))
                loadPlaylists()

                val missing = songs.size - found
                _importMessage.value = if (missing > 0) {
                    "Playlist \"$playlistName\" criada com ${unique.size} músicas.\n" +
                        "$missing não encontradas aqui no Groovix."
                } else {
                    "Playlist \"$playlistName\" criada com ${unique.size} músicas. Pronta pra tocar! 🎧"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _importMessage.value = "Falha ao importar: ${e.message ?: "erro desconhecido"}"
            } finally {
                _importProgress.value = null
            }
        }
    }

    fun cancelImport() {
        val job = importJob
        if (job?.isActive == true) {
            job.cancel()
            _importMessage.value = "Importação cancelada."
        }
        _importProgress.value = null
    }

    fun clearImportMessage() { _importMessage.value = null }

    /**
     * Quando o stream não veio (YouTube bloqueou o IP), procura uma versão da
     * mesma música em fonte que toca (SoundCloud/Audius).
     */
    private suspend fun findPlayableAlternative(track: Track): Track? {
        val results = try {
            playerRepo.search("${track.title} ${track.channel}".trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace(); emptyList()
        }
        return TrackMatcher.pickAlternative(results, track, track.title, track.channel)
    }

    /** Troca a faixa bloqueada pela que toca, na fila e nas listas da UI. */
    private fun swapCurrentTrack(old: Track, new: Track) {
        queue = queue.mapIndexed { i, t -> if (i == queueIndex) new else t }
        if (_tracks.value.any { it.id == old.id }) {
            _tracks.value = _tracks.value.map { if (it.id == old.id) new else it }
        }
        _currentTrack.value = new
        // Persiste o conserto em playlists e favoritos (na próxima já abre a boa)
        viewModelScope.launch(Dispatchers.IO) { repairSavedTracks(old, new) }
    }

    /** Substitui a faixa bloqueada pela que toca em todas as playlists/favoritos salvos. */
    private suspend fun repairSavedTracks(old: Track, new: Track) {
        try {
            dao.getAllPlaylists().forEach { playlist ->
                val tracks = try {
                    gson.fromJson(playlist.tracksJson, Array<Track>::class.java)?.toMutableList()
                } catch (_: Exception) { null } ?: return@forEach
                val idx = tracks.indexOfFirst { it.id == old.id }
                if (idx < 0) return@forEach
                tracks[idx] = new
                dao.insertPlaylist(playlist.copy(tracksJson = gson.toJson(tracks)))
            }
            if (dao.isFavorite(old.id)) {
                dao.deleteFavorite(old.id)
                dao.insertFavorite(FavoriteEntity(
                    trackId = new.id, id = new.id, title = new.title,
                    channel = new.channel, duration = new.duration,
                    thumbnail = new.thumbnail, url = new.url
                ))
            }
            loadPlaylists()
            loadFavorites()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Busca oficial YouTube Data API v3. Passe a key via BuildConfig/local.properties.
    fun searchYoutube(apiKey: String) {
        if (_query.value.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = RetrofitClient.youtubeApi.search(
                    query = _query.value,
                    apiKey = apiKey
                )
                _tracks.value = res.items.mapNotNull { it.toTrack() }
            } catch (e: Exception) {
                e.printStackTrace()
                _playbackError.value = "Falha na busca do YouTube"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isYoutubeTrack(track: Track): Boolean = TrackMatcher.isYoutube(track)

    fun playTrack(track: Track) {
        try {
            queue = _tracks.value.ifEmpty { listOf(track) }
            queueIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            resetShuffle()
            playCurrent()
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackError.value = "Não foi possível iniciar a faixa."
        }
    }

    fun playQueue(tracks: List<Track>, start: Track) {
        try {
            if (tracks.isEmpty()) return
            queue = tracks
            queueIndex = tracks.indexOfFirst { it.id == start.id }.coerceAtLeast(0)
            resetShuffle()
            _tracks.value = tracks
            playCurrent()
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackError.value = "Não foi possível iniciar a fila."
        }
    }

    private fun advanceIndex() {
        if (queue.isEmpty()) return
        if (_shuffle.value) {
            if (shuffleOrder.isEmpty()) buildShuffleOrder()
            shufflePos = (shufflePos + 1) % shuffleOrder.size
            queueIndex = shuffleOrder[shufflePos]
        } else {
            queueIndex = (queueIndex + 1) % queue.size
        }
    }

    private fun retreatIndex() {
        if (queue.isEmpty()) return
        if (_shuffle.value) {
            if (shuffleOrder.isEmpty()) buildShuffleOrder()
            shufflePos = (shufflePos - 1 + shuffleOrder.size) % shuffleOrder.size
            queueIndex = shuffleOrder[shufflePos]
        } else {
            queueIndex = (queueIndex - 1 + queue.size) % queue.size
        }
    }

    fun next() {
        try {
            if (queue.isEmpty()) return
            advanceIndex()
            playCurrent()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun prev() {
        try {
            if (queue.isEmpty()) return
            val c = controller
            if (c != null && try { c.currentPosition } catch (_: Exception) { 0L } > 3000L) {
                try { c.seekTo(0) } catch (_: Exception) { }
                return
            }
            retreatIndex()
            playCurrent()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun toggleShuffle() { try { _shuffle.value = !_shuffle.value; if (_shuffle.value) buildShuffleOrder() else resetShuffle() } catch (e: Exception) { e.printStackTrace() } }
    fun toggleRepeat() { try { _repeat.value = !_repeat.value } catch (e: Exception) { e.printStackTrace() } }
    fun togglePlayPause() {
        try {
            val c = controller ?: return
            if (try { c.isPlaying } catch (_: Exception) { false }) try { c.pause() } catch (_: Exception) { }
            else try { c.play() } catch (_: Exception) { }
        } catch (e: Exception) { e.printStackTrace() }
    }
    fun clearError() { _playbackError.value = null }

    fun retryCurrent() {
        try {
            if (queue.isEmpty() || queueIndex !in queue.indices) return
            playCurrent()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.isFavorite(track.id)) {
                dao.deleteFavorite(track.id)
            } else {
                dao.insertFavorite(FavoriteEntity(
                    trackId = track.id, id = track.id, title = track.title,
                    channel = track.channel, duration = track.duration,
                    thumbnail = track.thumbnail, url = track.url
                ))
            }
            loadFavorites()
        }
    }

    fun isFavorite(track: Track): Boolean = _favorites.value.any { it.id == track.id }

    fun savePlaylist(name: String, tracks: List<Track>? = null) {
        if (name.isBlank()) return
        val src = tracks?.distinctBy { it.id } ?: emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaylist(PlaylistEntity(
                id = UUID.randomUUID().toString(), name = name,
                tracksJson = gson.toJson(src)
            ))
            loadPlaylists()
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(id)
            loadPlaylists()
        }
    }

    fun loadPlaylistTracks(playlist: Playlist) {
        if (playlist.tracks.isEmpty()) return
        playQueue(playlist.tracks, playlist.tracks.first())
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = dao.getAllPlaylists()
            val target = all.find { it.id == playlistId } ?: return@launch
            val currentTracks = gson.fromJson(target.tracksJson, Array<Track>::class.java)?.toMutableList() ?: mutableListOf()
            if (currentTracks.none { it.id == track.id }) {
                currentTracks.add(track)
                dao.insertPlaylist(target.copy(tracksJson = gson.toJson(currentTracks)))
            }
            loadPlaylists()
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = dao.getAllPlaylists()
            val target = all.find { it.id == playlistId } ?: return@launch
            val currentTracks = gson.fromJson(target.tracksJson, Array<Track>::class.java)?.toMutableList() ?: mutableListOf()
            currentTracks.removeAll { it.id == track.id }
            dao.insertPlaylist(target.copy(tracksJson = gson.toJson(currentTracks)))
            loadPlaylists()
        }
    }

    private suspend fun loadPlaylists() {
        val entities = dao.getAllPlaylists()
        _playlists.value = entities.map { e ->
            val tracks = try {
                gson.fromJson(e.tracksJson, Array<Track>::class.java)?.toList() ?: emptyList()
            } catch (_: Exception) { emptyList() }
            Playlist(id = e.id, name = e.name, tracks = tracks)
        }
    }

    private suspend fun loadFavorites() {
        _favorites.value = dao.getAllFavorites().map { it.toTrack() }
    }

    private fun saveRecentTrack(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertRecentTrack(RecentTrackEntity(
                trackId = track.id, id = track.id, title = track.title,
                channel = track.channel, duration = track.duration,
                thumbnail = track.thumbnail, url = track.url
            ))
            loadRecentTracks()
        }
    }

    private suspend fun loadRecentTracks() {
        _recentTracks.value = dao.getRecentTracks().map { it.toTrack() }
    }

    private suspend fun resolveUrl(track: Track): String? {
        resolvedUrls[track.id]?.let { return it }
        // 1) InnerTube music.player (estilo Muka) — direto googlevideo
        try {
            playerRepo.resolveStreamUrl(track)?.let { url ->
                resolvedUrls[track.id] = url
                return url
            }
        } catch (e: Exception) { e.printStackTrace() }
        // 2) fallback backend antigo
        return try {
            val response = api.getAudio(track.url)
            val url = response.streamUrl
            if (url != null) resolvedUrls[track.id] = url
            url
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun playCurrent() {
        if (queue.isEmpty() || queueIndex < 0 || queueIndex >= queue.size) return
        val track = queue[queueIndex]
        _currentTrack.value = track
        _playbackError.value = null
        viewModelScope.launch {
            var playing = track
            var streamUrl = resolveUrl(playing)
            if (streamUrl == null) {
                // YouTube bloqueou o IP (ou backend off): tenta uma versão da
                // mesma música em fonte que toca (SoundCloud/Audius)
                reportFail(playing, stage = "resolve", reason = "nenhuma fonte resolveu a stream")
                val alt = findPlayableAlternative(playing)
                if (alt != null) {
                    val altUrl = resolveUrl(alt)
                    if (altUrl != null) {
                        swapCurrentTrack(playing, alt)
                        playing = alt
                        streamUrl = altUrl
                        Toast.makeText(
                            getApplication<Application>(),
                            "Fonte original bloqueada — tocando versão de ${alt.channel.ifBlank { "outra fonte" }}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            if (streamUrl == null) {
                // NÃO pula sozinho (era isso que "passava todas da pasta"):
                // para na faixa e mostra o motivo para debug
                reportFail(playing, stage = "fallback", reason = "nem a alternativa tocou")
                _playbackError.value =
                    "Sem stream para ${playing.title} (${playing.id}). " +
                    "YouTube bloqueou o player neste IP ou backend off. Tente outra faixa ou Wi-Fi/4G."
                return@launch
            }
            _playbackError.value = null
            val item = MediaItem.Builder().setMediaId(playing.id).setUri(streamUrl).build()
            val c = controller
            if (c == null) {
                pendingPlay = {
                    controller?.setMediaItem(item)
                    controller?.prepare()
                    controller?.play()
                }
            } else {
                c.setMediaItem(item)
                c.prepare()
                c.play()
            }
            saveRecentTrack(playing)
        }
    }

    private fun buildShuffleOrder() {
        shuffleOrder = queue.indices.toMutableList().apply { shuffle() }
        shufflePos = shuffleOrder.indexOf(queueIndex).coerceAtLeast(0)
    }

    private fun resetShuffle() { shuffleOrder = emptyList(); shufflePos = -1 }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
