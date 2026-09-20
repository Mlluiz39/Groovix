package com.seuapp.music.data.api

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * InnerTube direto, mesmo caminho que o Muka usa por baixo do JsEngine:
 * youtubei/v1/search + player + next, sem API key oficial.
 *
 * Clientes usados:
 * - WEB_REMIX (music.youtube.com) para busca
 * - ANDROID_TESTSUITE para player (retorna URL direta, sem decipher)
 */
interface InnerTubeApi {

    @POST("youtubei/v1/search")
    suspend fun search(
        @Query("key") key: String = InnerTubeClient.WEB_KEY,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Body body: JsonObject
    ): JsonObject

    @POST("youtubei/v1/player")
    suspend fun player(
        @Query("key") key: String = InnerTubeClient.WEB_KEY,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Body body: JsonObject
    ): JsonObject

    @POST("youtubei/v1/next")
    suspend fun next(
        @Query("key") key: String = InnerTubeClient.WEB_KEY,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Body body: JsonObject
    ): JsonObject

    @GET
    suspend fun timedText(@Url url: String): String
}
