package su.afk.yummy.tv.source.anilibria.domain.model

/** Отрезок пропуска (опенинг) в секундах от начала серии. */
data class AniLibriaSkip(
    val startSec: Int,
    val endSec: Int,
)
