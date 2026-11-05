package com.seuapp.music.data.youtube

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Token que o YouTube exige para entregar URLs de mídia (`streamingData`).
 *
 * O Chromium/WebView executa o BotGuard exatamente como o Brave faz; o que
 * muda é que aqui o WebView fica escondido e só devolve o `poToken`. O
 * ExoPlayer continua tocando a URL, com background e tela desligada.
 */
data class PoTokenResult(
    val visitorData: String,
    val playerToken: String,
    val streamingToken: String?,
    val clientVersion: String,
)

object PoTokenProvider {
    private val initMutex = Mutex()
    private val tokenMutex = Mutex()

    @Volatile
    private var botGuard: BotGuardWebView? = null

    @Volatile
    private var visitorData: String? = null

    @Volatile
    private var clientVersion: String? = null

    @Volatile
    private var streamingToken: String? = null

    /**
     * `visitorData` da sessão web. Necessário até para clientes sem poToken
     * (o VISIONOS só responde válido com um visitorData real), por isso é
     * exposto separado do WebView/BotGuard.
     */
    suspend fun sessionVisitorData(context: Context): String? {
        visitorData?.let { return it }
        return try {
            withTimeout(12_000) {
                initMutex.withLock {
                    if (visitorData == null) {
                        val home = runCatching { fetchHome() }.getOrNull()
                            ?: return@withLock null
                        visitorData = home.first
                        clientVersion = home.second
                    }
                    visitorData
                }
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (t: Throwable) {
            Log.w(TAG, "visitorData indisponível: ${t.message}")
            null
        }
    }

    /**
     * Gera o par visitorData + poToken para um vídeo.
     * Retorna `null` quando o WebView/BotGuard não está disponível: o
     * chamador deve seguir com o fallback normal (servidor/Audius/SoundCloud).
     */
    suspend fun get(context: Context, videoId: String): PoTokenResult? {
        val app = context.applicationContext
        return try {
            withTimeout(40_000) {
                initMutex.withLock { ensureReady(app) }
                val vd = visitorData ?: return@withTimeout null
                val bg = botGuard ?: return@withTimeout null

                // O streaming token precisa ser gerado antes de qualquer token de
                // player (exigência do BotGuard). Ele é vinculado ao visitorData e
                // reaproveitado entre vídeos enquanto o WebView viver.
                val streaming = streamingToken ?: runCatching {
                    tokenMutex.withLock { bg.generate(vd) }
                }.getOrNull()?.also { streamingToken = it }

                // O token do player é vinculado ao vídeo.
                val playerToken = tokenMutex.withLock { bg.generate(videoId) }

                PoTokenResult(
                    visitorData = vd,
                    playerToken = playerToken,
                    streamingToken = streaming,
                    clientVersion = clientVersion ?: DEFAULT_CLIENT_VERSION,
                )
            }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (t: Throwable) {
            Log.w(TAG, "poToken indisponível: ${t.message}")
            null
        }
    }

    private suspend fun ensureReady(context: Context) {
        if (visitorData == null) {
            val home = runCatching { fetchHome() }.getOrNull()
            if (home != null) {
                visitorData = home.first
                clientVersion = home.second
            }
        }
        if (visitorData == null) error("sem visitorData (home do YouTube não respondeu)")

        val current = botGuard
        if (current != null && !current.isExpired) return

        withContext(Dispatchers.Main) {
            current?.close()
            botGuard = null
            streamingToken = null // o token antigo é vinculado ao WebView anterior
            val fresh = BotGuardWebView(context)
            botGuard = fresh
            fresh.start()
        }

        try {
            // Não deixa o initMutex preso para sempre se o WebView travar.
            withTimeout(30_000) { botGuard?.awaitReady() }
        } catch (t: Throwable) {
            withContext(Dispatchers.Main) {
                botGuard?.close()
                botGuard = null
            }
            throw t
        }
    }

    private suspend fun fetchHome(): Pair<String, String>? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.youtube.com/?hl=pt&gl=BR")
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
            .build()
        HTTP.newCall(request).apply {
            // O withTimeout não interrompe chamada bloqueante do OkHttp.
            timeout().timeout(10, TimeUnit.SECONDS)
        }.execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val html = response.body?.string() ?: return@withContext null
            val visitor = Regex("\"VISITOR_DATA\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
                ?: return@withContext null
            val version = Regex("\"INNERTUBE_CLIENT_VERSION\":\"([^\"]+)\"")
                .find(html)?.groupValues?.get(1)
                ?: DEFAULT_CLIENT_VERSION
            visitor to version
        }
    }

    /**
     * WebView oculto que executa o BotGuard. Mantido vivo entre resoluções:
     * o token de integridade dura horas, então só o poToken por vídeo é novo.
     */
    private class BotGuardWebView(private val context: Context) {
        private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        private val ready = CompletableDeferred<Unit>()
        private val pending = mutableMapOf<String, CompletableDeferred<String>>()

        private var webView: WebView? = null

        @Volatile
        private var expiry: Long = 0L

        val isExpired: Boolean
            get() = expiry == 0L || System.currentTimeMillis() >= expiry

        fun start() {
            val view = WebView(context)
            webView = view
            view.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = USER_AGENT
                // O HTML é local; as duas chamadas do BotGuard são feitas pelo OkHttp.
                blockNetworkLoads = true
            }
            view.addJavascriptInterface(Bridge(), JS_INTERFACE)
            view.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    // Erro de sintaxe no HTML/injeção aparece aqui; loga para
                    // diagnosticar WebView antigo sem precisar de debugger.
                    if (message.messageLevel() != ConsoleMessage.MessageLevel.LOG) {
                        Log.w(TAG, "JS: ${message.message()} (${message.lineNumber()})")
                    }
                    return true
                }
            }
            view.webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?,
                ): Boolean {
                    Log.w(TAG, "renderer do WebView morreu")
                    failAll(IllegalStateException("renderer gone"))
                    return true
                }
            }

            val html = context.assets.open("po_token.html")
                .bufferedReader()
                .use { it.readText() }
                .replaceFirst("</script>", "\n$JS_INTERFACE.downloadAndRunBotguard()</script>")

            view.loadDataWithBaseURL(
                "https://www.youtube.com",
                html,
                "text/html",
                "utf-8",
                null,
            )
        }

        suspend fun awaitReady() = ready.await()

        suspend fun generate(identifier: String): String {
            if (isExpired) error("token de integridade expirado")
            val view = webView ?: error("WebView fechado")

            val deferred = CompletableDeferred<String>()
            synchronized(pending) { pending[identifier] = deferred }

            val u8 = stringToU8(identifier)
            val idJs = JSONObject.quote(identifier)
            withContext(Dispatchers.Main) {
                view.evaluateJavascript(
                    "try {" +
                        "var __t=obtainPoToken(window.webPoSignalOutput,window.integrityToken,$u8);" +
                        "var __o=[];for(var i=0;i<__t.length;i++){__o.push(__t[i]);}" +
                        "$JS_INTERFACE.onPoTokenResult($idJs,__o.join(','));" +
                        "}catch(e){" +
                        "$JS_INTERFACE.onPoTokenError($idJs,String(e));" +
                        "}",
                    null,
                )
            }
            return try {
                withTimeout(15_000) { deferred.await() }
            } finally {
                synchronized(pending) { pending.remove(identifier) }
            }
        }

        fun close() {
            scope.cancel()
            failAll(IllegalStateException("WebView fechado"))
            val view = webView
            webView = null
            if (view != null) {
                runCatching {
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.onPause()
                    view.removeAllViews()
                    view.destroy()
                }
            }
        }

        /** Libera quem estiver esperando um token quando o WebView morre/fecha. */
        private fun failAll(cause: Throwable) {
            if (!ready.isCompleted) ready.completeExceptionally(cause)
            val waiting = synchronized(pending) {
                val copy = pending.values.toList()
                pending.clear()
                copy
            }
            waiting.forEach { it.completeExceptionally(cause) }
        }

        private inner class Bridge {

            @JavascriptInterface
            fun downloadAndRunBotguard() {
                scope.launch { obtainChallenge() }
            }

            @JavascriptInterface
            fun onBotguardResult(botguardResponse: String) {
                scope.launch { exchangeIntegrityToken(botguardResponse) }
            }

            @JavascriptInterface
            fun onJsError(message: String) {
                Log.e(TAG, "erro de JS: $message")
                if (!ready.isCompleted) {
                    ready.completeExceptionally(IllegalStateException(message))
                }
            }

            @JavascriptInterface
            fun onPoTokenResult(identifier: String, u8: String) {
                val deferred = synchronized(pending) { pending.remove(identifier) } ?: return
                runCatching { u8ToBase64(u8) }
                    .onSuccess { deferred.complete(it) }
                    .onFailure { deferred.completeExceptionally(it) }
            }

            @JavascriptInterface
            fun onPoTokenError(identifier: String, message: String) {
                val deferred = synchronized(pending) { pending.remove(identifier) } ?: return
                deferred.completeExceptionally(IllegalStateException(message))
            }
        }

        private suspend fun obtainChallenge() {
            try {
                val raw = postJson(CREATE_URL, JSONArray().put(REQUEST_KEY).toString())
                val challenge = parseChallengeData(raw)
                withContext(Dispatchers.Main) {
                    val script = "try{" +
                        "runBotGuard($challenge).then(" +
                        "function(r){window.webPoSignalOutput=r.webPoSignalOutput;" +
                        "$JS_INTERFACE.onBotguardResult(r.botguardResponse);}," +
                        "function(e){$JS_INTERFACE.onJsError(String(e));}" +
                        ");" +
                        "}catch(e){$JS_INTERFACE.onJsError(String(e));}"
                    webView?.evaluateJavascript(script, null)
                }
            } catch (t: Throwable) {
                if (!ready.isCompleted) ready.completeExceptionally(t)
            }
        }

        private suspend fun exchangeIntegrityToken(botguardResponse: String) {
            try {
                val body = JSONArray()
                    .put(REQUEST_KEY)
                    .put(botguardResponse)
                    .toString()
                val raw = postJson(GENERATE_IT_URL, body)
                val (integrityTokenU8, expiresIn) = parseIntegrityTokenData(raw)

                val ttl = if (expiresIn > 700) expiresIn - 600 else 3600L
                expiry = System.currentTimeMillis() + ttl * 1000L

                withContext(Dispatchers.Main) {
                    webView?.evaluateJavascript(
                        "window.integrityToken=$integrityTokenU8;",
                        null,
                    )
                }
                ready.complete(Unit)
            } catch (t: Throwable) {
                if (!ready.isCompleted) ready.completeExceptionally(t)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// BotGuard / InnerTube helpers
// ---------------------------------------------------------------------------

private const val TAG = "GroovixPoToken"
private const val JS_INTERFACE = "GroovixPoToken"

private const val CREATE_URL = "https://www.youtube.com/api/jnn/v1/Create"
private const val GENERATE_IT_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"
private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw" // NOSONAR chave pública do BotGuard

private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

private const val DEFAULT_CLIENT_VERSION = "2.20240101.00.00"

private val HTTP: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()
}

private suspend fun postJson(url: String, body: String): String = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json+protobuf")
        .header("x-goog-api-key", GOOGLE_API_KEY)
        .header("x-user-agent", "grpc-web-javascript/0.1")
        .post(body.toRequestBody("application/json+protobuf".toMediaType()))
        .build()

    HTTP.newCall(request).execute().use { response ->
        val text = response.body?.string()
        if (!response.isSuccessful) {
            error("HTTP ${response.code}: ${text?.take(200)}")
        }
        text ?: error("resposta vazia")
    }
}

/**
 * O endpoint Create devolve um array embaralhado; o desafio real pode estar
 * em [1] (base64 com +97 por byte) ou em [0].
 */
private fun parseChallengeData(raw: String): String {
    val scrambled = JSONArray(raw)
    val data = if (scrambled.length() > 1 && scrambled.opt(1) is String) {
        JSONArray(descramble(scrambled.getString(1)))
    } else {
        scrambled.getJSONArray(0)
    }

    val messageId = data.getString(0)
    val interpreterHash = data.getString(3)
    val program = data.getString(4)
    val globalName = data.getString(5)
    val blob = data.optString(7, "")

    val interpreter = data.optJSONArray(1)
        ?.let { array ->
            (0 until array.length())
                .map { array.opt(it) }
                .firstOrNull { it is String } as? String
        }
        ?: error("desafio sem interpreterJavascript")

    val trustedUrl = data.optJSONArray(2)
        ?.let { array ->
            (0 until array.length())
                .map { array.opt(it) }
                .firstOrNull { it is String } as? String
        }

    return JSONObject()
        .put("messageId", messageId)
        .put(
            "interpreterJavascript",
            JSONObject().put(
                "privateDoNotAccessOrElseSafeScriptWrappedValue",
                interpreter,
            ).put(
                "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue",
                trustedUrl,
            ),
        )
        .put("interpreterHash", interpreterHash)
        .put("program", program)
        .put("globalName", globalName)
        .put("clientExperimentsStateBlob", blob)
        .toString()
}

/** GenerateIT devolve `["<base64>", <segundos>]`. */
private fun parseIntegrityTokenData(raw: String): Pair<String, Long> {
    val data = JSONArray(raw)
    return base64ToU8(data.getString(0)) to data.optLong(1, 0L)
}

/** String -> literal JS `new Uint8Array([...])` (bytes UTF-8). */
private fun stringToU8(value: String): String {
    val bytes = value.toByteArray(Charsets.UTF_8)
    return "new Uint8Array([" +
        bytes.joinToString(",") { (it.toInt() and 0xFF).toString() } +
        "])"
}

/** "97,98,99" (Uint8Array.toString) -> base64 com o alfabeto do YouTube. */
private fun u8ToBase64(csv: String): String {
    val bytes = csv.split(',')
        .map { it.trim().toInt().toByte() }
        .toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
        .replace('+', '-')
        .replace('/', '_')
}

/** Base64 do YouTube -> literal JS `new Uint8Array([...])`. */
private fun base64ToU8(value: String): String {
    val bytes = decodeYouTubeBase64(value)
    return "new Uint8Array([" +
        bytes.joinToString(",") { (it.toInt() and 0xFF).toString() } +
        "])"
}

/** Desafio embaralhado: base64 + 97 em cada byte. */
private fun descramble(value: String): String {
    val bytes = decodeYouTubeBase64(value)
    val decoded = ByteArray(bytes.size) { index -> (bytes[index] + 97).toByte() }
    return String(decoded, Charsets.UTF_8)
}

/**
 * Base64 "do YouTube": alfabeto URL-safe, às vezes com '.' como padding e
 * quase sempre sem os '=' finais. `android.util.Base64` recusa comprimento
 * que não seja múltiplo de 4, então normaliza e completa antes de decodificar.
 */
private fun decodeYouTubeBase64(value: String): ByteArray {
    var normalized = value
        .replace('-', '+')
        .replace('_', '/')
        .replace('.', '=')
        .trim()
    val remainder = normalized.length % 4
    if (remainder != 0) normalized += "=".repeat(4 - remainder)
    return Base64.decode(normalized, Base64.NO_WRAP)
}
