package su.afk.yummy.tv.source.amedia.domain.model

/** Потоки серии. [masterUrl] — мастер-плейлист HLS; [hlsByQuality] — высота кадра → плейлист этого качества (пусто, если разобрать не вышло). */
data class AmediaStreams(
    val masterUrl: String,
    val hlsByQuality: Map<Int, String>,
)
