package com.seuapp.music.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP com os mesmos headers que o Muka envia
 * (visto em PlatformHttpServiceImpl / NetworkManagerHotfix):
 * Chrome 126 Windows + Origin/Referer youtube.
 */
object InnerTubeClient {

    const val BASE_URL = "https://www.youtube.com/"
    const val WEB_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.6478.61 Safari/537.36"

    private val headerInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", UA)
            .header("Origin", "https://www.youtube.com")
            .header("Referer", "https://www.youtube.com/")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", "1.20240101.00.00")
            .build()
        chain.proceed(req)
    }

    private val okHttp: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(log)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val api: InnerTubeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            // Scalars primeiro para o timedText (String), Gson para o resto
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InnerTubeApi::class.java)
    }
}
