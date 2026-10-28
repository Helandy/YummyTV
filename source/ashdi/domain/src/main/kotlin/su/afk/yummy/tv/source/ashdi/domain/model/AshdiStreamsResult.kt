package su.afk.yummy.tv.source.ashdi.domain.model

/** Итог разрешения страницы плеера Ashdi в поток. */
sealed interface AshdiStreamsResult {
    data class Success(val streams: AshdiStreams) : AshdiStreamsResult
    data object NotFound : AshdiStreamsResult

    /** Плеер не отдаёт видео в стране пользователя (гео-блок Ashdi): серия есть, но с этим адресом её не посмотреть. */
    data object RegionBlocked : AshdiStreamsResult
    data class Failed(val reason: String?) : AshdiStreamsResult
}
