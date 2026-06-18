package com.seuapp.music.ui.viewmodel

import android.app.Application
import android.content.ComponentName
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
import com.seuapp.music.data.local.FavoriteEntity
import com.seuapp.music.data.local.MusicDatabase
import com.seuapp.music.data.local.PlaylistEntity
import com.seuapp.music.data.local.RecentTrackEntity
import com.seuapp.music.data.local.UserEntity
import com.seuapp.music.data.model.Playlist
import com.seuapp.music.data.model.Track
import com.seuapp.music.player.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val api = RetrofitClient.api
    private val gson = Gson()
    private val db = MusicDatabase.getDatabase(app)
    private val dao = db.musicDao()

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
        }
    }

    init {
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
            loadPlaylists()
            loadFavorites()
            loadRecentTracks()
            loadUser()
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
            try { _tracks.value = api.search(_query.value).results }
            catch (e: Exception) { e.printStackTrace() }
            finally { _isLoading.value = false }
        }
    }

    fun playTrack(track: Track) {
        queue = _tracks.value
        queueIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        resetShuffle()
        resolvedUrls.clear()
        playCurrent()
    }

    fun playQueue(tracks: List<Track>, start: Track) {
        queue = tracks
        queueIndex = tracks.indexOfFirst { it.id == start.id }.coerceAtLeast(0)
        resetShuffle()
        _tracks.value = tracks
        resolvedUrls.clear()
        playCurrent()
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
        if (queue.isEmpty()) return
        advanceIndex()
        playCurrent()
    }

    fun prev() {
        if (queue.isEmpty()) return
        val c = controller
        if (c != null && c.currentPosition > 3000L) { c.seekTo(0); return }
        retreatIndex()
        playCurrent()
    }

    fun toggleShuffle() { _shuffle.value = !_shuffle.value; if (_shuffle.value) buildShuffleOrder() else resetShuffle() }
    fun toggleRepeat() { _repeat.value = !_repeat.value }
    fun togglePlayPause() { val c = controller ?: return; if (c.isPlaying) c.pause() else c.play() }
    fun clearError() { _playbackError.value = null }

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
        return try {
            val response = api.getAudio(track.url)
            val url = response.streamUrl
            if (url != null) resolvedUrls[track.id] = url
            url
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun playCurrent(autoSkipTried: Int = 0) {
        if (queue.isEmpty() || queueIndex < 0 || queueIndex >= queue.size) return
        val track = queue[queueIndex]
        _currentTrack.value = track
        viewModelScope.launch {
            val streamUrl = resolveUrl(track)
            if (streamUrl == null) {
                if (autoSkipTried < queue.size) {
                    advanceIndex()
                    playCurrent(autoSkipTried + 1)
                } else {
                    _playbackError.value = "Não foi possível reproduzir esta faixa"
                }
                return@launch
            }
            _playbackError.value = null
            val item = MediaItem.Builder().setMediaId(track.id).setUri(streamUrl).build()
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
            saveRecentTrack(track)
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
