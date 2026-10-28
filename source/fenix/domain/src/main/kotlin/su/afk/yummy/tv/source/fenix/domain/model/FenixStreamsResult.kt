package su.afk.yummy.tv.source.fenix.domain.model

/** Итог разрешения страницы плеера Fenix в поток. */
sealed interface FenixStreamsResult {
    data class Success(val streams: FenixStreams) : FenixStreamsResult
    data object NotFound : FenixStreamsResult
    data class Failed(val reason: String?) : FenixStreamsResult
}
