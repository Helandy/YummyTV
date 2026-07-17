package su.afk.yummy.tv.data.search.storage.mapper

import su.afk.yummy.tv.core.storage.search.SearchFilterOptionsCache
import su.afk.yummy.tv.core.storage.search.SearchFilterOptionsEntry
import su.afk.yummy.tv.core.storage.search.SearchGenreEntry
import su.afk.yummy.tv.core.storage.search.SearchGenreGroupEntry
import su.afk.yummy.tv.core.storage.search.SearchItemEntry
import su.afk.yummy.tv.core.storage.search.SearchPageCache
import su.afk.yummy.tv.core.storage.search.SearchPageEntry
import su.afk.yummy.tv.core.storage.search.SearchTypeEntry
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.search.dto.YaniSearchCatalogDto
import su.afk.yummy.tv.data.search.dto.YaniSearchGenresDto
import su.afk.yummy.tv.data.search.dto.YaniSearchItemDto
import su.afk.yummy.tv.domain.search.model.SearchAnimeType
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchGenre
import su.afk.yummy.tv.domain.search.model.SearchGenreGroup
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchPage

internal fun List<YaniSearchItemDto>.toSearchPageCache(
    pageKey: String,
    language: String,
    limit: Int,
    offset: Int,
    responseSize: Int,
    cachedAt: Long,
): SearchPageCache =
    SearchPageCache(
        entry = SearchPageEntry(
            pageKey = pageKey,
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
            SearchItemEntry(
                pageKey = pageKey,
                position = index,
                animeId = animeId,
                title = item.title,
                posterUrl = item.poster?.run { medium ?: big ?: fullsize ?: small }?.toHttpsUrl(),
                rating = item.rating?.average,
                year = item.year,
            )
        },
    )

internal fun SearchPageCache.toSearchPage(): SearchPage =
    SearchPage(
        items = items
            .sortedBy { it.position }
            .map {
                SearchItem(
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

internal fun YaniSearchGenresDto.toSearchFilterOptionsCache(
    catalog: YaniSearchCatalogDto,
    language: String,
    cachedAt: Long,
): SearchFilterOptionsCache =
    SearchFilterOptionsCache(
        entry = SearchFilterOptionsEntry(
            language = language,
            cachedAt = cachedAt,
        ),
        genreGroups = groups.mapNotNull { group ->
            val id = group.id ?: return@mapNotNull null
            id to group
        }.mapIndexed { index, (id, group) ->
            SearchGenreGroupEntry(
                language = language,
                position = index,
                groupId = id,
                title = group.title,
            )
        },
        genres = genres.mapNotNull { genre ->
            val id = genre.value ?: return@mapNotNull null
            id to genre
        }.mapIndexed { index, (id, genre) ->
            SearchGenreEntry(
                language = language,
                position = index,
                genreId = id.toString(),
                title = genre.title,
                groupId = genre.groupId ?: 0,
            )
        },
        types = catalog.types.mapNotNull { item ->
            val type = item.type ?: return@mapNotNull null
            val id = type.alias ?: type.value?.toString() ?: return@mapNotNull null
            id to type
        }.mapIndexed { index, (id, type) ->
            SearchTypeEntry(
                language = language,
                position = index,
                typeId = id,
                title = type.name,
            )
        },
    )


internal fun SearchFilterOptionsCache.toSearchFilterOptions(): SearchFilterOptions =
    SearchFilterOptions(
        genreGroups = genreGroups
            .sortedBy { it.position }
            .map { SearchGenreGroup(id = it.groupId, title = it.title) },
        genres = genres
            .sortedBy { it.position }
            .map { SearchGenre(id = it.genreId, title = it.title, groupId = it.groupId) },
        types = types
            .sortedBy { it.position }
            .map { SearchAnimeType(id = it.typeId, title = it.title) },
    )
