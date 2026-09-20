package com.seuapp.music.data.innerTube

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.seuapp.music.data.api.AudiusApi
import com.seuapp.music.data.model.Track

/**
 * Fonte Audius — máxima extração do padrão Muka que FUNCIONA sem PO Token:
 * search + stream direto, sem key, sem quota, sem decipher.
 * Stream: GET /v1/tracks/{id}/stream?app_name=Groovix (302 -> gateway).
 */
class AudiusDataSource(
    private val api: AudiusApi
) {
    companion object {
        const val APP = "Groovix"
        const val PREFIX = "audius:"
    }

    suspend fun search(query: String, limit: Int = 25): List<Track> {
        val res = api.search(query = query, appName = APP, limit = limit)
        val arr = res.getAsJsonArray("data") ?: return emptyList()
        return arr.mapNotNull { parseTrack(it) }
    }

    suspend fun trending(limit: Int = 25): List<Track> {
        val res = api.trending(appName = APP, limit = limit)
        val arr = res.getAsJsonArray("data") ?: return emptyList()
        return arr.mapNotNull { parseTrack(it) }
    }

    fun streamUrl(audiusId: String): String {
        val clean = audiusId.removePrefix(PREFIX)
        return "https://discoveryprovider2.audius.co/v1/tracks/$clean/stream?app_name=$APP"
    }

    fun isAudius(track: Track): Boolean =
        track.id.startsWith(PREFIX) || track.url.contains("audius.co")

    private fun parseTrack(el: JsonElement?): Track? {
        if (el == null || !el.isJsonObject) return null
        val o = el.asJsonObject
        val id = o.optStr("id") ?: return null
        val title = o.optStr("title") ?: id
        val user = if (o.has("user") && o.get("user").isJsonObject) o.getAsJsonObject("user") else null
        val channel = user?.optStr("handle")?.let { "@$it" }
            ?: user?.optStr("name")
            ?: o.optStr("genre") ?: ""
        val durationMs = o.optLong("duration")?.let { s ->
            // Audius retorna segundos
            if (s in 1..36000) s * 1000 else null
        }
        val thumb = bestArtwork(o)
            ?: "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"

        return Track(
            id = "$PREFIX$id",
            title = title,
            channel = channel,
            duration = durationMs,
            thumbnail = thumb,
            url = streamUrl(id)
        )
    }

    private fun bestArtwork(o: JsonObject): String? {
        if (!o.has("artwork") || o.get("artwork").isJsonNull) return null
        return try {
            val art = o.getAsJsonObject("artwork")
            art.optStr("1000x1000") ?: art.optStr("480x480") ?: art.optStr("150x150")
        } catch (_: Exception) { null }
    }

    private fun JsonObject.optStr(name: String): String? {
        if (!has(name) || get(name).isJsonNull) return null
        return try { get(name).asString } catch (_: Exception) { null }
    }

    private fun JsonObject.optLong(name: String): Long? {
        if (!has(name) || get(name).isJsonNull) return null
        return try { get(name).asLong } catch (_: Exception) {
            try { get(name).asString.toLong() } catch (_: Exception) { null }
        }
    }
}
