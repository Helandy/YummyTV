package su.afk.yummy.tv.source.anistar.domain.model

/** Итог разрешения серии в поток. */
sealed interface AnistarStreamsResult {
    data class Success(val streams: AnistarStreams) : AnistarStreamsResult
    data object NotFound : AnistarStreamsResult
    data class Failed(val reason: String?) : AnistarStreamsResult
}
