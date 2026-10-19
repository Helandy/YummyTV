package su.afk.yummy.tv.data.catalog.mapper

import su.afk.yummy.tv.core.utils.network.toHttpsUrl
import su.afk.yummy.tv.data.catalog.dto.YaniSearchItemDto
import su.afk.yummy.tv.domain.catalog.model.SearchItem

internal fun YaniSearchItemDto.toSearchItem(): SearchItem? {
    val id = animeId ?: return null
    return SearchItem(
        id = id,
        title = title,
        posterUrl = poster?.run { medium ?: big ?: fullsize ?: small }?.toHttpsUrl(),
        rating = rating?.average?.takeIf { it > 0.0 },
        year = year,
    )
}
