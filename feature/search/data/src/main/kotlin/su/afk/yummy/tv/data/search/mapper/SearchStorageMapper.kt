package su.afk.yummy.tv.data.search.mapper

import su.afk.yummy.tv.core.storage.search.SearchFilterOptionsCache
import su.afk.yummy.tv.core.storage.search.SearchFilterOptionsEntry
import su.afk.yummy.tv.core.storage.search.SearchGenreEntry
import su.afk.yummy.tv.core.storage.search.SearchGenreGroupEntry
import su.afk.yummy.tv.core.storage.search.SearchItemEntry
import su.afk.yummy.tv.core.storage.search.SearchPageCache
import su.afk.yummy.tv.core.storage.search.SearchPageEntry
import su.afk.yummy.tv.core.storage.search.SearchTypeEntry
import su.afk.yummy.tv.domain.search.model.SearchAnimeType
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchGenre
import su.afk.yummy.tv.domain.search.model.SearchGenreGroup
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchPage

internal fun List<SearchItem>.toSearchPageCache(
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
        items = mapIndexed { index, item ->
            SearchItemEntry(
                pageKey = pageKey,
                position = index,
                animeId = item.id,
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
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
                )
            },
        nextOffset = entry.offset + entry.responseSize,
        canLoadMore = entry.responseSize >= entry.limit,
    )

internal fun SearchFilterOptions.toSearchFilterOptionsCache(
    language: String,
    cachedAt: Long,
): SearchFilterOptionsCache =
    SearchFilterOptionsCache(
        entry = SearchFilterOptionsEntry(
            language = language,
            cachedAt = cachedAt,
        ),
        genreGroups = genreGroups.mapIndexed { index, group ->
            SearchGenreGroupEntry(
                language = language,
                position = index,
                groupId = group.id,
                title = group.title,
            )
        },
        genres = genres.mapIndexed { index, genre ->
            SearchGenreEntry(
                language = language,
                position = index,
                genreId = genre.id,
                title = genre.title,
                groupId = genre.groupId,
            )
        },
        types = types.mapIndexed { index, type ->
            SearchTypeEntry(
                language = language,
                position = index,
                typeId = type.id,
                title = type.title,
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
