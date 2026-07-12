package su.afk.yummy.tv.data.search.mapper

import su.afk.yummy.tv.data.search.dto.YaniSearchGenreDto
import su.afk.yummy.tv.data.search.dto.YaniSearchGenreGroupDto
import su.afk.yummy.tv.data.search.dto.YaniSearchItemDto
import su.afk.yummy.tv.data.search.dto.YaniSearchTypeCountDto
import su.afk.yummy.tv.domain.search.model.SearchAnimeType
import su.afk.yummy.tv.domain.search.model.SearchGenre
import su.afk.yummy.tv.domain.search.model.SearchGenreGroup
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

internal fun YaniSearchGenreGroupDto.toSearchGenreGroup(): SearchGenreGroup? {
    val id = id ?: return null
    return SearchGenreGroup(id = id, title = title)
}

internal fun YaniSearchGenreDto.toSearchGenre(): SearchGenre? {
    val id = value ?: return null
    return SearchGenre(
        id = id.toString(),
        title = title,
        groupId = groupId ?: 0,
    )
}

internal fun YaniSearchTypeCountDto.toSearchAnimeType(): SearchAnimeType? {
    val type = type ?: return null
    val id = type.alias ?: type.value?.toString() ?: return null
    return SearchAnimeType(id = id, title = type.name)
}

private fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}
