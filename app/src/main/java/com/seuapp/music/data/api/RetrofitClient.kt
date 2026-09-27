package com.seuapp.music.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    /**
     * URL padrão do backend (tunnel Cloudflare). Pode ser trocada pelo usuário
     * em Configurações -> "Servidor (Backend)", sem precisar recompilar o app.
     * Toda URL precisa terminar com "/" (regra do Retrofit).
     */
    const val DEFAULT_BASE_URL = "https://api-music.mlluizdevtech.qzz.io/"

    /**
     * Normaliza uma URL digitada: trim, força http(s), barra no final.
     * Retorna null se não for usável (sem host com ponto, esquema errado...).
     */
    fun normalizeBaseUrl(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val url = (if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t")
            .trimEnd('/')
        val hostWithPort = url.substringAfter("://", "").substringBefore("/")
        if (hostWithPort.isEmpty()) return null
        val host = hostWithPort.substringBefore(":")
        if (!host.contains(".") && host != "localhost") return null
        return "$url/"
    }

    // URL efetiva em uso (persistida em SharedPreferences no app)
    @Volatile private var baseUrl: String = DEFAULT_BASE_URL
    @Volatile private var apiInstance: MusicApi? = null

    /** Reconstruído a cada troca de URL — nunca cache de outra base. */
    val api: MusicApi
        get() = apiInstance ?: synchronized(this) {
            apiInstance ?: buildApi(baseUrl).also { apiInstance = it }
        }

    /** Troca a base URL. Retorna false se a URL for inválida (nada muda). */
    fun setBaseUrl(raw: String): Boolean {
        val normalized = normalizeBaseUrl(raw) ?: return false
        synchronized(this) {
            baseUrl = normalized
            apiInstance = null // força rebuild na próxima chamada
        }
        return true
    }

    fun currentBaseUrl(): String = baseUrl

    private fun buildApi(base: String): MusicApi =
        Retrofit.Builder()
            .baseUrl(base)
            .client(backendHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicApi::class.java)

    // IMPORTANTE: baseUrl do Retrofit precisa terminar com "/"
    // Backend próprio pode estar "dormindo" (Render free / Cloud Run cold start:
    // 30-60s pra acordar). Timeout longo + o repo tenta de novo sozinho.
    private val backendHttp: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // YouTube Data API v3 — crie a key no Google Cloud e restrinja ao app.
    // Não commite a key real: use local.properties / BuildConfig.
    private const val YOUTUBE_BASE_URL = "https://www.googleapis.com/"

    val youtubeApi: YoutubeApi by lazy {
        Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApi::class.java)
    }

    private const val AUDIUS_BASE_URL = "https://discoveryprovider2.audius.co/"

    val audiusApi: AudiusApi by lazy {
        Retrofit.Builder()
            .baseUrl(AUDIUS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AudiusApi::class.java)
    }
}
