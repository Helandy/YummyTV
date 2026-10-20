package su.afk.yummy.tv.source.anistar.domain.model

/**
 * Потоки серии. [hlsByQuality] — высота кадра → ссылка на HLS (360, 720…); ссылки живут недолго. [referer] — зеркало сайта:
 * CDN отдаёт HLS только с ним.
 */
data class AnistarStreams(
    val hlsByQuality: Map<Int, String>,
    val referer: String,
)
