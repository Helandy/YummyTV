package su.afk.yummy.tv.source.anilibria.domain.model

/** Итог разрешения серии в потоки. */
sealed interface AniLibriaStreamsResult {
    data class Success(val streams: AniLibriaStreams) : AniLibriaStreamsResult
    data object NotFound : AniLibriaStreamsResult
    data class Failed(val reason: String) : AniLibriaStreamsResult
}
