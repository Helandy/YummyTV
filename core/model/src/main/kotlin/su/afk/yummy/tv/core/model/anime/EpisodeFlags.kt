package su.afk.yummy.tv.core.model.anime

/** Пометки серии из MyAnimeList: филлер и/или рекап. */
data class EpisodeFlags(
    val filler: Boolean,
    val recap: Boolean,
)
