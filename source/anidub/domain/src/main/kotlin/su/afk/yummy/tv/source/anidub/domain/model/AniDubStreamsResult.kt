package su.afk.yummy.tv.source.anidub.domain.model

/** Итог разрешения серии в поток. */
sealed interface AniDubStreamsResult {
    data class Success(val streams: AniDubStreams) : AniDubStreamsResult
    data object NotFound : AniDubStreamsResult
    data class Failed(val reason: String?) : AniDubStreamsResult
}
