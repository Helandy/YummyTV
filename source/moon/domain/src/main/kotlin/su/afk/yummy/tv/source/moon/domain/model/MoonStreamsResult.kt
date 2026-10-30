package su.afk.yummy.tv.source.moon.domain.model

/** Итог разрешения страницы плеера Moon в поток. */
sealed interface MoonStreamsResult {
    data class Success(val streams: MoonStreams) : MoonStreamsResult
    data object NotFound : MoonStreamsResult
    data class Failed(val reason: String?) : MoonStreamsResult
}
