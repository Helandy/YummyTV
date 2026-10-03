package su.afk.yummy.tv.domain.player.model

/** A subtitle file (`tracks` entry of the `bnsi` response), served as an external side-loaded track. */
data class AllohaSubtitleTrack(
    val label: String,
    val url: String,
    val language: String?,
    /** Lowercase file extension of the original source (`vtt`, `srt`, ...); [url] may be proxied. */
    val format: String? = null,
)
