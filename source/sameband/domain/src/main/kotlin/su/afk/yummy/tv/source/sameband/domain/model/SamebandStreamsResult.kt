package su.afk.yummy.tv.source.sameband.domain.model

/** Итог разрешения серии в поток. */
sealed interface SamebandStreamsResult {
    data class Success(val streams: SamebandStreams) : SamebandStreamsResult
    data object NotFound : SamebandStreamsResult
    data class Failed(val reason: String?) : SamebandStreamsResult
}
