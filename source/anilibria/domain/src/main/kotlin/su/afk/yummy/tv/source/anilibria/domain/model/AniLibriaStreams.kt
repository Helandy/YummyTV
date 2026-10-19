package su.afk.yummy.tv.source.anilibria.domain.model

/** Потоки серии: качество (высота в пикселях) → ссылка на HLS-плейлист, по возрастанию качества. */
data class AniLibriaStreams(
    val files: Map<Int, String>,
)
