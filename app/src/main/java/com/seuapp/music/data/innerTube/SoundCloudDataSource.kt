package com.seuapp.music.data.innerTube

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.seuapp.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fonte SoundCloud — funk/BR mainstream de verdade, sem servidor e sem key.
 * client_id é descoberto em runtime (igual ao yt-dlp faz): baixa a home,
 * varre os bundles JS e testa cada candidato de 32 chars até um responder.
 * Stream: transcoding progressive -> MP3 assinado (ExoPlayer toca direto).
 */
class SoundCloudDataSource(
    private val gson: Gson = Gson()
) {
    companion object {
        const val PREFIX = "soundcloud:"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.6478.61 Safari/537.36"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()
    private var clientId: String? = null

    suspend fun search(query: String, limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        val cid = ensureClientId() ?: return@withContext emptyList()
        try {
            val url = "https://api-v2.soundcloud.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&client_id=$cid&limit=$limit"
            val body = get(url) ?: return@withContext emptyList()
            val root = gson.fromJson(body, JsonObject::class.java)
            val col = if (root.has("collection") && root.get("collection").isJsonArray)
                root.getAsJsonArray("collection") else return@withContext emptyList()
            col.mapNotNull { parseTrack(it.asJsonObject) }
        } catch (e: Exception) {
            e.printStackTrace()
            // client_id pode ter rodado: limpa e tenta 1x com descoberta nova
            mutex.withLock { clientId = null }
            try {
                val cid2 = ensureClientId() ?: return@withContext emptyList()
                val url = "https://api-v2.soundcloud.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                    "&client_id=$cid2&limit=$limit"
                val body = get(url) ?: return@withContext emptyList()
                val root = gson.fromJson(body, JsonObject::class.java)
                val col = if (root.has("collection") && root.get("collection").isJsonArray)
                    root.getAsJsonArray("collection") else return@withContext emptyList()
                col.mapNotNull { parseTrack(it.asJsonObject) }
            } catch (e2: Exception) {
                e2.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun resolveStream(track: Track): String? = withContext(Dispatchers.IO) {
        val cid = ensureClientId() ?: return@withContext null
        val numId = track.id.removePrefix(PREFIX)
        try {
            val meta = get("https://api-v2.soundcloud.com/tracks/$numId?client_id=$cid")
                ?: return@withContext null
            val root = gson.fromJson(meta, JsonObject::class.java)
            val trs = root.getAsJsonObject("media")?.getAsJsonArray("transcodings")
                ?: return@withContext null
            var prog: String? = null
            for (i in 0 until trs.size()) {
                val t = trs[i].asJsonObject
                val proto = t.getAsJsonObject("format")?.get("protocol")?.asString
                if (proto == "progressive") { prog = t.get("url")?.asString; break }
            }
            prog = prog ?: return@withContext null
            val js = get("$prog?client_id=$cid") ?: return@withContext null
            gson.fromJson(js, JsonObject::class.java).get("url")?.asString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isSoundCloud(track: Track): Boolean =
        track.id.startsWith(PREFIX) || track.url.contains("soundcloud.com")

    // ---------- client_id discovery ----------

    private suspend fun ensureClientId(): String? {
        mutex.withLock { clientId?.let { return it } }
        return try {
            val home = get("https://soundcloud.com/") ?: return null
            val bundles = Regex("https://a-v2\\.sndcdn\\.com/assets/[^\"]+\\.js")
                .findAll(home).map { it.value }.distinct().toList()
            // bundles maiores primeiro (o client costuma estar nos grandes)
            for (b in bundles.sortedByDescending { it.length }) {
                val js = get(b) ?: continue
                val cands = Regex("client_id:\"([A-Za-z0-9]{32})\"")
                    .findAll(js).map { it.groupValues[1] }
                    .filter { !it.contains("google", ignoreCase = true) }
                    .distinct()
                for (c in cands) {
                    if (testClientId(c)) {
                        mutex.withLock { clientId = c }
                        return c
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun testClientId(cid: String): Boolean {
        return try {
            val body = get(
                "https://api-v2.soundcloud.com/search?q=teste&client_id=$cid&limit=1"
            ) ?: return false
            body.contains("\"collection\"")
        } catch (_: Exception) { false }
    }

    private fun get(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.string()
        }
    }

    private fun parseTrack(o: JsonObject): Track? {
        return try {
            if (o.get("kind")?.asString != "track") return null
            val id = o.get("id")?.asString ?: return null
            val title = o.get("title")?.asString ?: id
            val user = o.getAsJsonObject("user")
            val channel = user?.get("username")?.asString ?: ""
            val dur = try { o.get("duration")?.asLong } catch (_: Exception) { null }
            var art: String? = try { o.get("artwork_url")?.asString } catch (_: Exception) { null }
            if (art != null) art = art.replace("-large.", "-t500x500.")
            Track(
                id = "$PREFIX$id",
                title = title,
                channel = channel,
                duration = dur,
                thumbnail = art,
                url = try { o.get("permalink_url")?.asString } catch (_: Exception) { null }
                    ?: "https://soundcloud.com/"
            )
        } catch (_: Exception) { null }
    }
}
