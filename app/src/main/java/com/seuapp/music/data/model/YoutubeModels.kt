package com.seuapp.music.data.model

import com.google.gson.annotations.SerializedName

data class YoutubeSearchResponse(
    @SerializedName("items") val items: List<YoutubeItem> = emptyList()
)

data class YoutubeItem(
    @SerializedName("id") val id: YoutubeId?,
    @SerializedName("snippet") val snippet: YoutubeSnippet?
)

data class YoutubeId(
    @SerializedName("videoId") val videoId: String? = null
)

data class YoutubeSnippet(
    @SerializedName("title") val title: String? = null,
    @SerializedName("channelTitle") val channelTitle: String? = null,
    @SerializedName("thumbnails") val thumbnails: YoutubeThumbnails? = null
)

data class YoutubeThumbnails(
    @SerializedName("medium") val medium: YoutubeThumb? = null,
    @SerializedName("high") val high: YoutubeThumb? = null,
    @SerializedName("default") val default: YoutubeThumb? = null
)

data class YoutubeThumb(
    @SerializedName("url") val url: String? = null
)

fun YoutubeItem.toTrack(): Track? {
    val videoId = id?.videoId ?: return null
    val thumb = snippet?.thumbnails?.high?.url
        ?: snippet?.thumbnails?.medium?.url
        ?: snippet?.thumbnails?.default?.url
    return Track(
        id = videoId,
        title = snippet?.title ?: videoId,
        channel = snippet?.channelTitle ?: "",
        thumbnail = thumb,
        url = "https://www.youtube.com/watch?v=$videoId"
    )
}
