package com.seuapp.music.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track(
    val id: String,
    val title: String,
    val channel: String,
    val duration: Long? = null,
    val thumbnail: String? = null,
    val url: String
) : Parcelable

data class SearchResponse(val results: List<Track>)

data class AudioResponse(
    val title: String?,
    val channel: String?,
    val thumbnail: String?,
    val streamUrl: String?
)

data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<Track>
)
