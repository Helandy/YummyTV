package su.afk.yummy.tv.core.model.anime

data class AnimeVideoSkips(
    val opening: AnimeVideoSkipSegment? = null,
    val ending: AnimeVideoSkipSegment? = null,
)
