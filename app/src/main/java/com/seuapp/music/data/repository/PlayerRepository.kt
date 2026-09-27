package com.seuapp.music.data.repository

import com.seuapp.music.data.api.InnerTubeClient
import com.seuapp.music.data.api.RetrofitClient
import com.seuapp.music.data.innerTube.AudiusDataSource
import com.seuapp.music.data.innerTube.InnerTubeDataSource
import com.seuapp.music.data.innerTube.SoundCloudDataSource
import com.seuapp.music.data.model.AudioResponse
import com.seuapp.music.data.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Máximo do Muka aplicado sem erros:
 * - search: Audius (tocável) + InnerTube WEB_REMIX (descoberta) em paralelo,
 *   com fallbacks encadeados e nunca estoura exceção para a UI.
 * - resolve: Audius stream direto -> InnerTube player -> backend antigo.
 */
class PlayerRepository(
    private val inner: InnerTubeDataSource = InnerTubeDataSource(InnerTubeClient.api),
    private val audius: AudiusDataSource = AudiusDataSource(RetrofitClient.audiusApi),
    private val sc: SoundCloudDataSource = SoundCloudDataSource()
) {
    companion object {
        /** Falhas seguidas do InnerTube no dispositivo antes de pular essa etapa. */
        const val DEFAULT_YT_FAIL_LIMIT = 3

        /** Faixa aceitável pro limite vindo do /config (nunca 0 nem absurdo). */
        fun clampYtFailLimit(value: Int): Int = value.coerceIn(1, 20)
    }

    /**
     * Limite de falhas seguidas do YouTube no celular — default local, mas o
     * backend manda outro via GET /config (flag remota, sem recompilar).
     */
    @Volatile
    var ytFailLimit: Int = DEFAULT_YT_FAIL_LIMIT
        set(value) {
            field = clampYtFailLimit(value)
        }

    private val streamCache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    /**
     * Falhas seguidas resolvendo YouTube no celular. Quando o YouTube bloqueia
     * o IP, adianta nada tentar 3 clientes InnerTube por música em playlist
     * grande — depois de [ytFailLimit] seguidas, pula direto pro backend.
     * Zera sozinho quando alguma resolução funciona.
     */
    private val ytDeviceFails = AtomicInteger(0)

    suspend fun search(query: String): List<Track> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()
        val dAudius = async {
            try { audius.search(query) } catch (e: Exception) { e.printStackTrace(); emptyList() }
        }
        val dInner = async {
            try { inner.search(query) } catch (e: Exception) { e.printStackTrace(); emptyList() }
        }
        val dSc = async {
            try { sc.search(query) } catch (e: Exception) { e.printStackTrace(); emptyList() }
        }
        val a = dAudius.await()
        val b = dInner.await()
        val s = dSc.await()
        // SoundCloud primeiro (mainstream BR que toca), depois Audius,
        // depois YouTube (descoberta rica)
        val merged = LinkedHashMap<String, Track>()
        (s + a + b).forEach { merged.putIfAbsent(it.id, it) }
        if (merged.isEmpty()) {
            // último fallback: backend antigo (hoje 1033, mas pode voltar).
            // Tenta 2x com pausa: a 1ª acorda o servidor (cold start),
            // a 2ª pega o resultado. Nunca estoura exceção pra UI.
            try {
                RetrofitClient.api.search(query).results.forEach { merged.putIfAbsent(it.id, it) }
            } catch (e: Exception) { e.printStackTrace() }
            if (merged.isEmpty()) {
                try {
                    kotlinx.coroutines.delay(5000)
                    RetrofitClient.api.search(query).results.forEach { merged.putIfAbsent(it.id, it) }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        merged.values.toList()
    }

    suspend fun trending(): List<Track> {
        return try { audius.trending() } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    suspend fun getAudio(track: Track): AudioResponse? {
        val url = resolveStreamUrl(track) ?: return null
        return AudioResponse(
            title = track.title,
            channel = track.channel,
            thumbnail = track.thumbnail,
            streamUrl = url
        )
    }

    suspend fun resolveStreamUrl(track: Track): String? {
        mutex.withLock { streamCache[track.id]?.let { return it } }

        // 1) Audius: url direta, sem resolve (302 -> gateway). Sempre funciona.
        if (audius.isAudius(track)) {
            val id = track.id.removePrefix(AudiusDataSource.PREFIX)
            val url = try { audius.streamUrl(id) } catch (_: Exception) { track.url }
            mutex.withLock { streamCache[track.id] = url }
            return url
        }

        // 1b) SoundCloud: MP3 progressivo direto, sem servidor
        if (sc.isSoundCloud(track)) {
            try {
                sc.resolveStream(track)?.let { url ->
                    mutex.withLock { streamCache[track.id] = url }
                    return url
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // 2) InnerTube music.player (pode dar UNPLAYABLE sem PO Token — tenta mesmo assim).
        //    Se o YouTube já bloqueou o IP N vezes seguidas nesta sessão, pula:
        //    evita 3 tentativas inúteis por música quando se toca playlist grande.
        val vid = inner.extractVideoId(track.url.ifBlank { track.id })
        if (vid != null && ytDeviceFails.get() < ytFailLimit) {
            var resolved: String? = null
            try { resolved = inner.resolveStream(track.url.ifBlank { track.id })?.streamUrl }
            catch (e: Exception) { e.printStackTrace() }
            if (resolved == null) {
                try { resolved = inner.resolveStream(track.id)?.streamUrl }
                catch (e: Exception) { e.printStackTrace() }
            }
            if (resolved != null) {
                ytDeviceFails.set(0)
                mutex.withLock { streamCache[track.id] = resolved }
                return resolved
            }
            ytDeviceFails.incrementAndGet()
        }

        // 3) backend antigo
        return try {
            val url = RetrofitClient.api.getAudio(track.url).streamUrl
            if (url != null) mutex.withLock { streamCache[track.id] = url }
            url
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun related(track: Track): List<Track> =
        try { inner.related(track.url.ifBlank { track.id }) }
        catch (e: Exception) { e.printStackTrace(); emptyList() }

    fun cachedUrl(trackId: String): String? = streamCache[trackId]
}
