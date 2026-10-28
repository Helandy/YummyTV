package su.afk.yummy.tv.source.ailiberty.domain.model

/** Итог разрешения серии в поток. */
sealed interface AiLibertyStreamsResult {
    data class Success(val streams: AiLibertyStreams) : AiLibertyStreamsResult
    data object NotFound : AiLibertyStreamsResult
    data class Failed(val reason: String?) : AiLibertyStreamsResult
}
