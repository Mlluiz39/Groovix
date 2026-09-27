package com.seuapp.music.data.api

import com.seuapp.music.data.model.YoutubeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApi {
    @GET("youtube/v3/search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoCategoryId") videoCategoryId: String = "10",
        @Query("maxResults") maxResults: Int = 25,
        @Query("q") query: String,
        @Query("key") apiKey: String
    ): YoutubeSearchResponse
}
