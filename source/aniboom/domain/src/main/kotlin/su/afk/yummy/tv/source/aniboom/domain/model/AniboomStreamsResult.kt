package su.afk.yummy.tv.source.aniboom.domain.model

/** Итог разрешения серии в поток. */
sealed interface AniboomStreamsResult {
    data class Success(val streams: AniboomStreams) : AniboomStreamsResult
    data object NotFound : AniboomStreamsResult
    data class Failed(val reason: String?) : AniboomStreamsResult
}
