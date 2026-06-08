package su.afk.yummy.tv.data.top.mapper

import su.afk.yummy.tv.data.top.dto.YaniAnimeTopItemDto
import su.afk.yummy.tv.domain.top.model.AnimeTopItem

internal fun YaniAnimeTopItemDto.toAnimeTopItem(): AnimeTopItem? {
    val id = animeId ?: return null
    return AnimeTopItem(
        id = id,
        title = title,
        posterUrl = poster?.run { medium ?: big ?: fullsize ?: small }?.toHttpsUrl(),
        rating = rating?.average,
    )
}

internal fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}
