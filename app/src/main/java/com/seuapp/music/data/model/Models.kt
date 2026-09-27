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

/**
 * Resposta do GET /health do backend.
 * FastAPI novo: {"ok": true, "time": ...} · Node antigo: {"status": "ok"}
 * (aceita os dois, senão "Salvar e testar" mostra ⚠️ com o servidor certo).
 */
data class HealthResponse(
    val ok: Boolean? = null,
    val status: String? = null,
    val time: Long? = null
) {
    val healthy: Boolean get() = ok == true || status == "ok"
}

/**
 * Resposta do GET /config do backend — flags de estratégia remotas.
 * Tudo opcional: campo ausente (ou backend antigo sem /config) mantém o
 * default do app.
 */
data class RemoteConfigResponse(
    val version: Int? = null,
    /** Falhas seguidas do InnerTube no celular antes de pular pro servidor. */
    val ytFailLimit: Int? = null,
    /** Se o servidor valida a stream antes de entregar (Range/204). */
    val validateStream: Boolean? = null,
    /** Se o app pode reportar falhas em POST /api/fail. */
    val telemetry: Boolean? = null
)

/** POST /api/fail — relato de falha de playback (telemetria). */
data class FailReport(
    val track: String,
    val artist: String,
    val source: String,
    val stage: String,
    val reason: String,
    val app: String = "android"
)

data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<Track>
)
