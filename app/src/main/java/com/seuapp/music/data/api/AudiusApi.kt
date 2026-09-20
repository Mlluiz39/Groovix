package com.seuapp.music.data.api

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Fonte que realmente toca (testado: 200 OK + 302 p/ stream).
 * Usada como playback garantido enquanto o youtubei bloqueia player
 * (UNPLAYABLE/400 mesmo no Wi-Fi/4G — exige PO Token que só o
 * backend do Muka tem via api.ddsiiwid.com).
 */
interface AudiusApi {
    @GET("v1/tracks/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("app_name") appName: String = "Groovix",
        @Query("limit") limit: Int = 25
    ): JsonObject

    @GET("v1/tracks/trending")
    suspend fun trending(
        @Query("app_name") appName: String = "Groovix",
        @Query("genre") genre: String? = null,
        @Query("limit") limit: Int = 25
    ): JsonObject

    @GET("v1/tracks/{id}")
    suspend fun track(
        @Path("id") id: String,
        @Query("app_name") appName: String = "Groovix"
    ): JsonObject
}
