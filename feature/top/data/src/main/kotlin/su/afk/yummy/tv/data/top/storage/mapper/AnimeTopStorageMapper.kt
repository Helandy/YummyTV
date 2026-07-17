package su.afk.yummy.tv.data.top.storage.mapper

import su.afk.yummy.tv.core.storage.top.AnimeTopItemEntry
import su.afk.yummy.tv.core.storage.top.AnimeTopPageCache
import su.afk.yummy.tv.core.storage.top.AnimeTopPageEntry
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.top.dto.YaniAnimeTopItemDto
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType

internal fun List<YaniAnimeTopItemDto>.toAnimeTopPageCache(
    type: AnimeTopType,
    language: String,
    limit: Int,
    offset: Int,
    responseSize: Int,
    cachedAt: Long,
): AnimeTopPageCache =
    AnimeTopPageCache(
        entry = AnimeTopPageEntry(
            type = type.apiValue,
            language = language,
            limit = limit,
            offset = offset,
            responseSize = responseSize,
            cachedAt = cachedAt,
        ),
        items = mapNotNull { item ->
            val animeId = item.animeId ?: return@mapNotNull null
            animeId to item
        }.mapIndexed { index, (animeId, item) ->
            AnimeTopItemEntry(
                type = type.apiValue,
                language = language,
                limit = limit,
                offset = offset,
                position = index,
                animeId = animeId,
                title = item.title,
                posterUrl = item.poster?.run { medium ?: big ?: fullsize ?: small }?.toHttpsUrl(),
                rating = item.rating?.average,
                year = item.year,
            )
        },
    )


internal fun AnimeTopPageCache.toAnimeTopPage(): AnimeTopPage =
    AnimeTopPage(
        items = items
            .sortedBy { it.position }
            .map {
                AnimeTopItem(
                    id = it.animeId,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    rating = it.rating,
                    year = it.year,
                )
            },
        nextOffset = entry.offset + entry.responseSize,
        canLoadMore = entry.responseSize >= entry.limit,
    )
