package com.seuapp.music.data.api

import com.seuapp.music.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MusicApi {
    @GET("api/search")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("api/audio")
    suspend fun getAudio(@Query("url") url: String): AudioResponse

    /** Health do backend — usado em Configurações pra testar a URL do servidor. */
    @GET("health")
    suspend fun health(): HealthResponse

    /** Config remota (flags de estratégia) — busca no start e ao testar o servidor. */
    @GET("config")
    suspend fun config(): RemoteConfigResponse

    /** Telemetria: avisa o backend quando uma faixa falhou (fire-and-forget). */
    @POST("api/fail")
    suspend fun reportFail(@Body report: FailReport)
}
