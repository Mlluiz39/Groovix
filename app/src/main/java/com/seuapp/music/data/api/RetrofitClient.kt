package com.seuapp.music.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // IMPORTANTE: baseUrl do Retrofit precisa terminar com "/"
    private const val BASE_URL = "https://api-music.mlluizdevtech.qzz.io/"

    // Backend próprio pode estar "dormindo" (Render free / Cloud Run cold start:
    // 30-60s pra acordar). Timeout longo + o repo tenta de novo sozinho.
    private val backendHttp: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val api: MusicApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(backendHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicApi::class.java)
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
