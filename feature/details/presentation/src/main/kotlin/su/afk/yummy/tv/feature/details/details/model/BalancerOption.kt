package su.afk.yummy.tv.feature.details.details.model

import su.afk.yummy.tv.core.model.anime.AnimeVideo

data class BalancerOption(
    val playerName: String,
    val video: AnimeVideo,
    val isSupported: Boolean = true,
    /** Число серий этой озвучки у этого балансера по всему тайтлу. */
    val episodeCount: Int = 0,
)
