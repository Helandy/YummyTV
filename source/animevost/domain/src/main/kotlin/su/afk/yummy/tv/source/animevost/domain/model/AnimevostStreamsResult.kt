package su.afk.yummy.tv.source.animevost.domain.model

/** Итог разрешения серии в поток. */
sealed interface AnimevostStreamsResult {
    data class Success(val streams: AnimevostStreams) : AnimevostStreamsResult
    data object NotFound : AnimevostStreamsResult
    data class Failed(val reason: String?) : AnimevostStreamsResult
}
