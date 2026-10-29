package su.afk.yummy.tv.feature.details.details.model

import su.afk.yummy.tv.core.model.anime.AnimeVideo

data class BalancerOption(
    val playerName: String,
    val video: AnimeVideo,
    val isSupported: Boolean = true,
    /** Число серий этой озвучки у этого балансера по всему тайтлу. */
    val episodeCount: Int = 0,
    /** Просмотры балансера по всему тайтлу — по ним сортируется пикер. */
    val views: Int = 0,
    /** У балансера стоит звезда: такие идут в пикере первыми. */
    val isStarred: Boolean = false,
    /** Позиция в порядке без звёзд: по ней балансер возвращается на место, когда звезду снимают. */
    val baseIndex: Int = 0,
)
