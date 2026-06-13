package com.seuapp.music.data.api

import com.seuapp.music.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {
    @GET("api/search")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("api/audio")
    suspend fun getAudio(@Query("url") url: String): AudioResponse
}
