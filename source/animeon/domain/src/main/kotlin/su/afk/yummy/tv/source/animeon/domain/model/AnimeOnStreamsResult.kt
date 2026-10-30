package su.afk.yummy.tv.source.animeon.domain.model

/** Итог разрешения серии AnimeON в поток. */
sealed interface AnimeOnStreamsResult {
    data class Success(val streams: AnimeOnStreams) : AnimeOnStreamsResult
    data object NotFound : AnimeOnStreamsResult
    data object RegionBlocked : AnimeOnStreamsResult
    data class Failed(val reason: String?) : AnimeOnStreamsResult
}
