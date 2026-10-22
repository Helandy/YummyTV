package su.afk.yummy.tv.source.amedia.domain.model

/** Итог разрешения серии в поток. */
sealed interface AmediaStreamsResult {
    data class Success(val streams: AmediaStreams) : AmediaStreamsResult
    data object NotFound : AmediaStreamsResult
    data class Failed(val reason: String?) : AmediaStreamsResult
}
