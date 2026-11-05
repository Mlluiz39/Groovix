package com.seuapp.music.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP dos endpoints InnerTube.
 *
 * Os headers `X-Youtube-Client-Name/Version` precisam combinar com o
 * `context.client` do body, senão o YouTube responde num client diferente do
 * pedido (era o que acontecia: tudo ia como WEB_REMIX 1.20240101).
 * Por isso eles são opcionais aqui e cada chamada sobrescreve quando precisa.
 */
object InnerTubeClient {

    const val BASE_URL = "https://www.youtube.com/"
    const val WEB_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    /** WEB_REMIX: usado por search/next. */
    const val DEFAULT_CLIENT_ID = "67"
    const val DEFAULT_CLIENT_VERSION = "1.20240101.00.00"

    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.6478.61 Safari/537.36"

    private val headerInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        // Só preenche o que a chamada não definiu.
        if (original.header("User-Agent") == null) builder.header("User-Agent", DESKTOP_UA)
        if (original.header("Origin") == null) builder.header("Origin", "https://www.youtube.com")
        if (original.header("Referer") == null) builder.header("Referer", "https://www.youtube.com/")
        if (original.header("Accept-Language") == null) {
            builder.header("Accept-Language", "en-US,en;q=0.9")
        }
        if (original.header("X-Youtube-Client-Name") == null) {
            builder.header("X-Youtube-Client-Name", DEFAULT_CLIENT_ID)
        }
        if (original.header("X-Youtube-Client-Version") == null) {
            builder.header("X-Youtube-Client-Version", DEFAULT_CLIENT_VERSION)
        }

        chain.proceed(builder.build())
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
