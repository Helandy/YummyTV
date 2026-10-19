package su.afk.yummy.tv.source.anilibria.domain.model

/** Адрес серии в источнике: релиз и порядковый номер серии. */
data class AniLibriaTarget(
    val releaseId: Int,
    val ordinal: Int,
)
