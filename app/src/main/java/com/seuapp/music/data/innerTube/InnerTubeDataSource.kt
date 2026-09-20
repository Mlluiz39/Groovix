package com.seuapp.music.data.innerTube

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.seuapp.music.data.api.InnerTubeApi
import com.seuapp.music.data.model.Track

/**
 * Equivalente aos DataServices do Muka:
 * - MusicSearchDataService (music.search)
 * - MusicDetailDataService (music.player / music.watchNext / music.lyrics)
 *
 * Tudo via InnerTube, sem depender do dex/JS baixado do api.ddsiiwid.com.
 */
class InnerTubeDataSource(
    private val api: InnerTubeApi
) {

    // ---------- bodies ----------

    private fun baseClient(clientName: String, clientVersion: String, extra: Map<String, Any?> = emptyMap()): JsonObject {
        val client = JsonObject().apply {
            addProperty("clientName", clientName)
            addProperty("clientVersion", clientVersion)
            addProperty("hl", "pt")
            addProperty("gl", "BR")
            addProperty("originalUrl", "https://music.youtube.com/")
            addProperty(
                "userAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.6478.61 Safari/537.36"
            )
            extra.forEach { (k, v) ->
                when (v) {
                    is String -> addProperty(k, v)
                    is Number -> addProperty(k, v)
                    else -> Unit
                }
            }
        }
        return JsonObject().apply {
            add("client", client)
        }
    }

    private fun searchBody(query: String): JsonObject {
        val body = JsonObject()
        body.addProperty("query", query)
        // sem "params": busca geral igual ao Muka (com filtro vinha 0 resultados)
        body.add("context", baseClient("WEB_REMIX", "1.20240101.00.00"))
        return body
    }

    private fun playerBody(videoId: String, clientName: String, clientVersion: String): JsonObject {
        val body = JsonObject()
        body.addProperty("videoId", videoId)
        val context = JsonObject().apply {
            val client = JsonObject().apply {
                addProperty("clientName", clientName)
                addProperty("clientVersion", clientVersion)
                if (clientName.startsWith("ANDROID")) {
                    addProperty("androidSdkVersion", 34)
                } else {
                    addProperty(
                        "userAgent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/126.0.6478.61 Safari/537.36"
                    )
                    addProperty("originalUrl", "https://music.youtube.com/")
                }
                addProperty("hl", "pt")
                addProperty("gl", "BR")
            }
            add("client", client)
        }
        body.add("context", context)
        val cpb = JsonObject()
        cpb.addProperty("html5Preference", "HTML5_PREF_WANTS")
        body.add("contentPlaybackContext", cpb)
        body.addProperty("contentCheckOk", true)
        body.addProperty("racyCheckOk", true)
        return body
    }

    private fun nextBody(videoId: String): JsonObject {
        val body = JsonObject()
        body.addProperty("videoId", videoId)
        body.add("context", baseClient("WEB_REMIX", "1.20240101.00.00"))
        body.addProperty("autonavState", "STATE_ON")
        return body
    }

    // ---------- search ----------

    suspend fun search(query: String): List<Track> {
        val res = api.search(body = searchBody(query))
        val out = LinkedHashMap<String, Track>()
        collectSearchItems(res, out)
        return out.values.toList()
    }

    private fun collectSearchItems(el: JsonElement?, out: LinkedHashMap<String, Track>) {
        if (el == null || el.isJsonNull) return
        if (el.isJsonObject) {
            val obj = el.asJsonObject
            if (obj.has("musicResponsiveListItemRenderer")) {
                parseMusicItem(obj.getAsJsonObject("musicResponsiveListItemRenderer"))?.let {
                    out.putIfAbsent(it.id, it)
                }
            }
            if (obj.has("videoRenderer")) {
                parseVideoRenderer(obj.getAsJsonObject("videoRenderer"))?.let {
                    out.putIfAbsent(it.id, it)
                }
            }
            obj.entrySet().forEach { (_, v) -> collectSearchItems(v, out) }
        } else if (el.isJsonArray) {
            el.asJsonArray.forEach { collectSearchItems(it, out) }
        }
    }

    private fun JsonArray.safeGet(i: Int): JsonElement? =
        if (i in 0 until size()) get(i) else null

    private fun JsonObject.obj(name: String): JsonObject? =
        if (has(name) && get(name).isJsonObject) getAsJsonObject(name) else null

    private fun JsonObject.arr(name: String): JsonArray? =
        if (has(name) && get(name).isJsonArray) getAsJsonArray(name) else null

    private fun JsonObject.str(name: String): String? =
        if (has(name) && !get(name).isJsonNull) try { get(name).asString } catch (_: Exception) { null } else null

    private fun runsText(parent: JsonObject?, name: String = "runs"): JsonArray? =
        parent?.obj("text")?.arr(name)

    private fun parseMusicItem(r: JsonObject): Track? {
        // navigationEndpoint.watchEndpoint.videoId
        var videoId = r.obj("navigationEndpoint")?.obj("watchEndpoint")?.str("videoId")
        if (videoId == null) {
            val flexCols = r.arr("flexColumns")
            if (flexCols != null) {
                outer@ for (i in 0 until flexCols.size()) {
                    val col = flexCols[i]
                    if (!col.isJsonObject) continue
                    val runs = col.asJsonObject
                        .obj("musicResponsiveListItemFlexColumnRenderer")
                        ?.let { runsText(it) } ?: continue
                    for (j in 0 until runs.size()) {
                        val run = runs[j]
                        if (!run.isJsonObject) continue
                        val id = run.asJsonObject
                            .obj("navigationEndpoint")?.obj("watchEndpoint")?.str("videoId")
                        if (id != null) { videoId = id; break@outer }
                    }
                }
            }
        }
        val id = videoId ?: return null

        val flex = r.arr("flexColumns")
        var title: String = id
        var channel = ""
        var duration: Long? = null
        if (flex != null && flex.size() > 0) {
            val col0 = flex.safeGet(0)
            if (col0 != null && col0.isJsonObject) {
                val runs0 = col0.asJsonObject
                    .obj("musicResponsiveListItemFlexColumnRenderer")
                    ?.let { runsText(it) }
                val t = runs0?.safeGet(0)
                if (t != null && t.isJsonObject) {
                    t.asJsonObject.str("text")?.let { title = it }
                }
            }
            if (flex.size() > 1) {
                val col1 = flex.safeGet(1)
                if (col1 != null && col1.isJsonObject) {
                    val runs1 = col1.asJsonObject
                        .obj("musicResponsiveListItemFlexColumnRenderer")
                        ?.let { runsText(it) }
                    if (runs1 != null && runs1.size() > 0) {
                        runs1.safeGet(0)?.let {
                            if (it.isJsonObject) it.asJsonObject.str("text")?.let { c -> channel = c }
                        }
                        runs1.safeGet(runs1.size() - 1)?.let {
                            if (it.isJsonObject) it.asJsonObject.str("text")?.let { d -> duration = parseDurationToMs(d) }
                        }
                    }
                }
            }
        }

        var thumb: String? = null
        r.obj("thumbnail")?.obj("musicThumbnailRenderer")?.obj("thumbnail")?.arr("thumbnails")?.let { arr ->
            if (arr.size() > 0) {
                val last = arr[arr.size() - 1]
                if (last.isJsonObject) {
                    thumb = last.asJsonObject.str("url")?.let { u -> if (u.startsWith("http")) u else "https:$u" }
                }
            }
        }

        return Track(
            id = id,
            title = title,
            channel = channel,
            duration = duration,
            thumbnail = thumb ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            url = "https://www.youtube.com/watch?v=$id"
        )
    }

    private fun parseVideoRenderer(r: JsonObject): Track? {
        val videoId = r.str("videoId") ?: return null
        var title: String = videoId
        r.obj("title")?.let { t ->
            t.arr("runs")?.safeGet(0)?.let {
                if (it.isJsonObject) it.asJsonObject.str("text")?.let { s -> title = s }
            } ?: t.str("simpleText")?.let { title = it }
        }
        var channel = ""
        r.obj("ownerText")?.arr("runs")?.safeGet(0)?.let {
            if (it.isJsonObject) it.asJsonObject.str("text")?.let { s -> channel = s }
        }
        if (channel.isEmpty()) {
            r.obj("longBylineText")?.arr("runs")?.safeGet(0)?.let {
                if (it.isJsonObject) it.asJsonObject.str("text")?.let { s -> channel = s }
            }
        }
        var thumb: String? = null
        r.obj("thumbnail")?.arr("thumbnails")?.let { arr ->
            if (arr.size() > 0) {
                val last = arr[arr.size() - 1]
                if (last.isJsonObject) thumb = last.asJsonObject.str("url")
            }
        }
        val duration = r.obj("lengthText")?.str("simpleText")?.let { parseDurationToMs(it) }
        return Track(
            id = videoId,
            title = title,
            channel = channel,
            duration = duration,
            thumbnail = thumb ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            url = "https://www.youtube.com/watch?v=$videoId"
        )
    }

    // ---------- player (music.player do Muka) ----------

    data class StreamInfo(
        val title: String,
        val author: String,
        val lengthMs: Long,
        val streamUrl: String,
        val mimeType: String,
        val itag: Int,
        val isLive: Boolean = false
    )

    suspend fun resolveStream(videoIdOrUrl: String): StreamInfo? {
        val videoId = extractVideoId(videoIdOrUrl) ?: return null
        // Cada cliente tem sua key (bug anterior usava a key WEB pra tudo -> 400).
        // Ordem: ANDROID primeiro (url direta), depois WEB_REMIX, depois TV.
        val attempts = listOf(
            Triple("ANDROID", "19.09.37", "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ"),
            Triple("WEB_REMIX", "1.20240101.00.00", "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"),
            Triple("TVHTML5", "7.20240101.00.00", "AIzaSyDc7l7vT-8QbR0F8v0q0q0q0q0q0q0q0q0")
        )
        for ((cli, ver, key) in attempts) {
            try {
                val res = api.player(key = key, body = playerBody(videoId, cli, ver))
                val parsed = parsePlayer(res, videoId)
                if (parsed != null) return parsed
            } catch (e: Exception) {
                // tenta o próximo cliente (400/404/UNPLAYABLE)
                continue
            }
        }
        return null
    }

    suspend fun related(videoIdOrUrl: String): List<Track> {
        val videoId = extractVideoId(videoIdOrUrl) ?: return emptyList()
        return try {
            val res = api.next(body = nextBody(videoId))
            val out = LinkedHashMap<String, Track>()
            collectSearchItems(res, out)
            out.values.filter { it.id != videoId }.take(25)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parsePlayer(root: JsonObject, fallbackId: String): StreamInfo? {
        // respeita playability igual ao Muka (BusinessPlayerInfo.status)
        val playability = root.obj("playabilityStatus")?.str("status")
        if (playability != null && playability != "OK") return null
        val details = root.obj("videoDetails")
        val title = details?.get("title")?.asString ?: fallbackId
        val author = details?.get("author")?.asString ?: ""
        val lengthMs = details?.get("lengthSeconds")?.asString?.toLongOrNull()?.times(1000)
            ?: 0L
        val isLive = details?.get("isLive")?.asBoolean ?: false

        val streaming = root.getAsJsonObject("streamingData") ?: return null
        val formats = streaming.getAsJsonArray("formats") ?: JsonArray()
        val adaptive = streaming.getAsJsonArray("adaptiveFormats") ?: JsonArray()
        val all = mutableListOf<JsonObject>()
        formats.forEach { if (it.isJsonObject) all.add(it.asJsonObject) }
        adaptive.forEach { if (it.isJsonObject) all.add(it.asJsonObject) }
        if (all.isEmpty()) return null

        // prioridade igual ao downloader SABR do Muka: áudio puro primeiro
        val prioItag = listOf(251, 140, 250, 249, 250, 139, 140, 22, 18, 43, 36, 17)
        fun score(o: JsonObject): Int {
            val itag = o.get("itag")?.asInt ?: 9999
            val idx = prioItag.indexOf(itag).let { if (it == -1) 500 else it }
            val mime = o.get("mimeType")?.asString ?: ""
            val audioBonus = if (mime.startsWith("audio/")) -100 else 0
            return idx * 10 + audioBonus
        }
        val best = all
            .filter { it.has("url") } // ANDROID_TESTSUITE já vem com url direta
            .minByOrNull { score(it) }
            ?: return null

        return StreamInfo(
            title = title,
            author = author,
            lengthMs = lengthMs,
            streamUrl = best.get("url").asString,
            mimeType = best.get("mimeType")?.asString ?: "audio/mp4",
            itag = best.get("itag")?.asInt ?: 0,
            isLive = isLive
        )
    }

    // ---------- helpers ----------

    fun extractVideoId(input: String): String? {
        Regex("[?&]v=([A-Za-z0-9_-]{11})").find(input)?.let { return it.groupValues[1] }
        Regex("(youtu\\.be/|/shorts/|/embed/|/live/|music\\.youtube\\.com/watch\\?.*v=)([A-Za-z0-9_-]{11})")
            .find(input)?.let { return it.groupValues[2] }
        if (input.matches(Regex("[A-Za-z0-9_-]{11}"))) return input
        // fallback Muka/Groovix: ids curtos internos
        if (input.matches(Regex("[A-Za-z0-9_-]{6,}")) && !input.contains("http")) return input
        return null
    }

    private fun parseDurationToMs(s: String): Long? {
        // "3:45" ou "1:02:10"
        return try {
            val parts = s.trim().split(":").map { it.toLong() }
            val ms = when (parts.size) {
                2 -> parts[0] * 60_000 + parts[1] * 1000
                3 -> parts[0] * 3_600_000 + parts[1] * 60_000 + parts[2] * 1000
                else -> return null
            }
            ms
        } catch (_: Exception) { null }
    }
}
