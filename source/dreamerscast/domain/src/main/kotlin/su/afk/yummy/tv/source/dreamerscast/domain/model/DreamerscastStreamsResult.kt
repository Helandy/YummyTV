package su.afk.yummy.tv.source.dreamerscast.domain.model

/** Итог разрешения серии в поток. */
sealed interface DreamerscastStreamsResult {
    data class Success(val streams: DreamerscastStreams) : DreamerscastStreamsResult
    data object NotFound : DreamerscastStreamsResult
    data class Failed(val reason: String?) : DreamerscastStreamsResult
}
