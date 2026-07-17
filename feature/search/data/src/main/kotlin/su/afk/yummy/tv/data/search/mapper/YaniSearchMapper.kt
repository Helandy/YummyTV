package su.afk.yummy.tv.data.search.mapper

import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.search.dto.YaniSearchItemDto
import su.afk.yummy.tv.domain.search.model.SearchItem

internal fun YaniSearchItemDto.toSearchItem(): SearchItem? {
    val id = animeId ?: return null
    return SearchItem(
        id = id,
        title = title,
        posterUrl = poster?.run { medium ?: big ?: fullsize ?: small }?.toHttpsUrl(),
        rating = rating?.average,
        year = year,
    )
}
