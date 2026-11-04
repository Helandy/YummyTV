package su.afk.yummy.tv.source.studiomir.domain.model

/** Итог разрешения серии в поток. */
sealed interface StudioMirStreamsResult {
    data class Success(val streams: StudioMirStreams) : StudioMirStreamsResult
    data object NotFound : StudioMirStreamsResult
    data class Failed(val reason: String?) : StudioMirStreamsResult
}
