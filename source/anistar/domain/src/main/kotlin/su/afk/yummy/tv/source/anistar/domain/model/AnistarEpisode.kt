package su.afk.yummy.tv.source.anistar.domain.model

/** Серия из плейлиста AniStar. [number] — номер из названия серии на сайте (нумерация сайта, у длинных тайтлов сквозная). */
data class AnistarEpisode(
    val number: Int,
)
