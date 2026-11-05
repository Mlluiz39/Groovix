package com.seuapp.music.data.innerTube

import android.content.Context
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.seuapp.music.data.api.InnerTubeApi
import com.seuapp.music.data.api.InnerTubeClient
import com.seuapp.music.data.model.Track
import com.seuapp.music.data.youtube.PoTokenProvider

/**
 * Equivalente aos DataServices do Muka:
 * - MusicSearchDataService (music.search)
 * - MusicDetailDataService (music.player / music.watchNext / music.lyrics)
 *
 * Tudo via InnerTube, sem depender do dex/JS baixado do api.ddsiiwid.com.
 */
class InnerTubeDataSource(
    private val api: InnerTubeApi,
    private val appContext: Context? = null
) {

    private companion object {
        const val TAG = "GroovixInnerTube"

        /**
         * Chave pública do web client (funciona para todos os clients InnerTube
         * atuais; a antiga chave ANDROID foi descontinuada).
         */
        const val WEB_KEY = InnerTubeClient.WEB_KEY

        const val DESKTOP_UA = InnerTubeClient.DESKTOP_UA

        const val VISIONOS_UA =
            "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS " +
                "26_6_0 like Mac OS X; BR)"

        const val ANDROID_UA =
            "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip"

        const val TV_UA =
            "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold " +
                "(unlike Gecko), Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)"

        const val WEB_EMBED_UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)"

        /**
         * VISIONOS devolve `url` direta e não exige poToken (é o cliente que o
         * NewPipeExtractor usa hoje). Primeira tentativa, sem custo de WebView.
         */
        val VISIONOS = ClientSpec(
            name = "VISIONOS",
            version = "1.04",
            clientId = "101",
            key = WEB_KEY,
            userAgent = VISIONOS_UA,
            osName = "visionOS",
            osVersion = "26.6.0.23O770",
            deviceMake = "Apple",
            deviceModel = "RealityDevice17,1",
            clientScreen = "WATCH",
            apiFormatVersion = "2",
        )

        /** Web/tv embedded: exigem o poToken gerado pelo BotGuard do WebView. */
        val TV_SIMPLY = ClientSpec(
            name = "TVHTML5_SIMPLY",
            version = "1.0",
            clientId = "75",
            key = WEB_KEY,
            embedded = true,
        )

        val WEB = ClientSpec(
            name = "WEB",
            version = "2.20240101.00.00", // substituído pela versão viva da home
            clientId = "1",
            key = WEB_KEY,
            userAgent = DESKTOP_UA,
        )

        val WEB_EMBEDDED = ClientSpec(
            name = "WEB_EMBEDDED_PLAYER",
            version = "2.20240101.00.00",
            clientId = "56",
            key = WEB_KEY,
            embedded = true,
            userAgent = WEB_EMBED_UA,
        )

        /** Sem poToken: só funcionam se o YouTube ainda liberar o IP. */
        val ANDROID_VR = ClientSpec(
            name = "ANDROID_VR",
            version = "1.65.10",
            clientId = "28",
            key = WEB_KEY,
            userAgent =
                "com.google.android.apps.youtube.vr.oculus/1.65.10 " +
                    "(Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            android = true,
            sdk = 32,
            osName = "Android",
            osVersion = "12L",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            gvsPot = true,
        )

        val ANDROID = ClientSpec(
            name = "ANDROID",
            version = "21.26.364",
            clientId = "3",
            key = WEB_KEY,
            userAgent = ANDROID_UA,
            android = true,
            sdk = 30,
            osName = "Android",
            osVersion = "11",
        )

        val TV = ClientSpec(
            name = "TVHTML5",
            version = "7.20260707.07.00",
            clientId = "7",
            key = WEB_KEY,
            userAgent = TV_UA,
        )

        val WEB_REMIX = ClientSpec(
            name = "WEB_REMIX",
            version = "1.20240101.00.00",
            clientId = "67",
            key = WEB_KEY,
        )
    }

    private data class ClientSpec(
        val name: String,
        val version: String,
        val clientId: String,
        val key: String,
        val userAgent: String? = null,
        val poToken: String? = null,
        val embedded: Boolean = false,
        val android: Boolean = false,
        val osName: String? = null,
        val osVersion: String? = null,
        val sdk: Int? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val clientScreen: String? = null,
        val apiFormatVersion: String? = null,
        /** Anexar o streaming poToken como `&pot=` na URL de mídia. */
        val gvsPot: Boolean = false,
    )

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

    private fun playerBody(
        videoId: String,
        spec: ClientSpec,
        visitorData: String? = null,
    ): JsonObject {
        val body = JsonObject()
        body.addProperty("videoId", videoId)
        // NewPipe/yt-dlp mandam um nonce de playback; sem ele alguns clientes
        // respondem com playabilityStatus estranho.
        body.addProperty("cpn", contentPlaybackNonce())

        val client = JsonObject().apply {
            addProperty("clientName", spec.name)
            addProperty("clientVersion", spec.version)
            addProperty("hl", "pt")
            addProperty("gl", "BR")
            visitorData?.let { addProperty("visitorData", it) }

            spec.userAgent?.let { addProperty("userAgent", it) }
            spec.deviceMake?.let { addProperty("deviceMake", it) }
            spec.deviceModel?.let { addProperty("deviceModel", it) }
            spec.osName?.let { addProperty("osName", it) }
            spec.osVersion?.let { addProperty("osVersion", it) }
            spec.sdk?.let { addProperty("androidSdkVersion", it) }

            // `originalUrl` só faz sentido para clients de navegador.
            if (!spec.android) addProperty("originalUrl", "https://music.youtube.com/")
            val screen = spec.clientScreen ?: if (spec.embedded) "EMBED" else null
            screen?.let { addProperty("clientScreen", it) }
        }

        body.add("context", JsonObject().apply { add("client", client) })

        if (spec.embedded) {
            body.add("thirdParty", JsonObject().apply {
                addProperty("embedUrl", "https://www.youtube.com/")
            })
        }

        val cpb = JsonObject()
        cpb.addProperty("html5Preference", "HTML5_PREF_WANTS")
        body.add("contentPlaybackContext", cpb)
        body.addProperty("contentCheckOk", true)
        body.addProperty("racyCheckOk", true)

        // Faz o cliente web/TV devolver URL de mídia em vez de só SABR.
        spec.poToken?.let { token ->
            body.add("serviceIntegrityDimensions", JsonObject().apply {
                addProperty("poToken", token)
            })
        }
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

        val ctx = appContext
        // O VISIONOS exige um visitorData válido mesmo sem poToken.
        val vd = if (ctx != null) {
            try {
                PoTokenProvider.sessionVisitorData(ctx)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w(TAG, "visitorData: ${e.message}")
                null
            }
        } else {
            null
        }

        // 1) VISIONOS: caminho do NewPipeExtractor atual. Devolve `url` direta
        //    e não exige poToken — por isso vem primeiro (resposta instantânea;
        //    o WebView do BotGuard só entra se este cliente falhar).
        play(videoId, VISIONOS, visitorData = vd)?.let { return it }

        // 2) Caminho atestado: o WebView gera o poToken (web) e os clients
        //    web/TV passam a devolver URL em vez de SABR.
        val pot = if (ctx != null) {
            try {
                PoTokenProvider.get(ctx, videoId)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (e: Exception) {
                Log.w(TAG, "poToken: ${e.message}")
                null
            }
        } else {
            null
        }

        val attempts = ArrayList<ClientSpec>(8)
        if (pot != null) {
            // Clients "web": aceitam o poToken do BotGuard e precisam do
            // streaming token anexado na URL (`&pot=`).
            attempts += TV_SIMPLY.copy(
                poToken = pot.playerToken,
                gvsPot = true,
            )
            attempts += WEB.copy(
                version = pot.clientVersion,
                poToken = pot.playerToken,
                gvsPot = true,
            )
            attempts += WEB_EMBEDDED.copy(
                version = pot.clientVersion,
                poToken = pot.playerToken,
                gvsPot = true,
            )
            // ANDROID_VR não usa player token, mas aceita o GVS token na URL
            // e (importante) não exige decifrar o parâmetro `n`.
            attempts += ANDROID_VR
        }
        // Fallbacks sem token: podem virar SABR/bloqueio, mas não custam nada.
        attempts += ANDROID
        attempts += TV
        attempts += WEB_REMIX

        for (spec in attempts) {
            play(videoId, spec, visitorData = pot?.visitorData, gvsToken = pot?.streamingToken)
                ?.let { return it }
        }
        return null
    }

    /** Nonce de playback de 16 chars (mesmo formato usado pelo player web). */
    private fun contentPlaybackNonce(): String =
        java.util.UUID.randomUUID().toString().replace("-", "").take(16)

    /** Uma tentativa de player; `null` significa "tenta o próximo cliente". */
    private suspend fun play(
        videoId: String,
        spec: ClientSpec,
        visitorData: String? = null,
        gvsToken: String? = null,
    ): StreamInfo? {
        return try {
            val response = api.player(
                key = spec.key,
                clientId = spec.clientId,
                clientVersion = spec.version,
                userAgent = spec.userAgent,
                apiFormatVersion = spec.apiFormatVersion,
                body = playerBody(videoId, spec, visitorData),
            )
            val parsed = parsePlayer(response, videoId)
            val status = response.obj("playabilityStatus")?.str("status")
            val reason = response.obj("playabilityStatus")?.str("reason")
            Log.i(
                TAG,
                "client=${spec.name} status=$status reason=${reason ?: "-"} " +
                    "url=${parsed != null} itag=${parsed?.itag ?: 0} " +
                    "sabr=${hasSabr(response)} pot=${spec.poToken != null}"
            )
            if (parsed != null && spec.gvsPot && gvsToken != null) {
                parsed.copy(streamUrl = appendPot(parsed.streamUrl, gvsToken))
            } else {
                parsed
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            // tenta o próximo cliente (400/404/UNPLAYABLE)
            Log.w(TAG, "client=${spec.name} falhou: ${e.message}")
            null
        }
    }

    /** O cliente respondeu com SABR (sem URL direta) em vez de formatos tocáveis. */
    private fun hasSabr(root: JsonObject): Boolean =
        root.obj("streamingData")?.has("serverAbrStreamingUrl") == true

    /** Acrescenta `pot` à URL de mídia sem duplicar se o player já mandou um. */
    private fun appendPot(url: String, poToken: String): String {
        if (url.contains("pot=")) return url
        val separator = if (url.contains('?')) '&' else '?'
        return url + separator + "pot=" + poToken
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
