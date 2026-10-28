package su.afk.yummy.tv.source.animebesst.domain.model

/** Итог разрешения серии в поток. */
sealed interface AnimebesstStreamsResult {
    data class Success(val streams: AnimebesstStreams) : AnimebesstStreamsResult
    data object NotFound : AnimebesstStreamsResult
    data class Failed(val reason: String?) : AnimebesstStreamsResult
}
