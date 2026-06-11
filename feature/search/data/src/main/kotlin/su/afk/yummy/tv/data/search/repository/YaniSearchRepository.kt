package su.afk.yummy.tv.data.search.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.search.dto.YaniSearchCatalogDto
import su.afk.yummy.tv.data.search.dto.YaniSearchGenresDto
import su.afk.yummy.tv.data.search.dto.YaniSearchResponseDto
import su.afk.yummy.tv.data.search.mapper.toSearchAnimeType
import su.afk.yummy.tv.data.search.mapper.toSearchGenre
import su.afk.yummy.tv.data.search.mapper.toSearchGenreGroup
import su.afk.yummy.tv.data.search.mapper.toSearchItem
import su.afk.yummy.tv.data.search.network.YaniSearchApi
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchPage
import su.afk.yummy.tv.domain.search.repository.SearchRepository

private const val SEARCH_FILTER_OPTIONS_TTL_MS = 24 * 60 * 60 * 1000L
private const val SEARCH_RESULTS_TTL_MS = 10 * 60 * 1000L

@Serializable
private data class YaniSearchFilterOptionsDto(
    val genres: YaniSearchGenresDto = YaniSearchGenresDto(),
    val catalog: YaniSearchCatalogDto = YaniSearchCatalogDto(),
)

class YaniSearchRepository(
    private val api: YaniSearchApi,
    private val cache: CacheStore,
    private val json: Json,
) : SearchRepository {
    override suspend fun search(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): SearchPage = withContext(Dispatchers.IO) {
        val response = cache.getOrFetch(
            key = searchCacheKey(query, filters, limit, offset),
            ttlMs = SEARCH_RESULTS_TTL_MS,
            serialize = { dto: YaniSearchResponseDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { YaniSearchResponseDto(response = api.search(query, filters, limit, offset)) },
        ).response
        SearchPage(
            items = response.mapNotNull { it.toSearchItem() },
            nextOffset = offset + response.size,
            canLoadMore = response.size >= limit,
        )
    }

    override suspend fun getFilterOptions(): SearchFilterOptions = withContext(Dispatchers.IO) {
        val response = cache.getOrFetch(
            key = "search_filter_options_v1",
            ttlMs = SEARCH_FILTER_OPTIONS_TTL_MS,
            serialize = { dto: YaniSearchFilterOptionsDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = {
                coroutineScope {
                    val genres = async { api.getGenres() }
                    val catalog = async { api.getCatalog() }
                    YaniSearchFilterOptionsDto(
                        genres = genres.await(),
                        catalog = catalog.await(),
                    )
                }
            },
        )
        with(response) {
            SearchFilterOptions(
                genreGroups = genres.groups.mapNotNull { it.toSearchGenreGroup() },
                genres = genres.genres.mapNotNull { it.toSearchGenre() },
                types = catalog.types.mapNotNull { it.toSearchAnimeType() },
            )
        }
    }

    private fun searchCacheKey(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): String = buildString {
        append("search_results_v1")
        append("_q=").append(query.trim().lowercase())
        append("_genres=").append(filters.genres.sorted().joinToString(","))
        append("_excluded=").append(filters.excludedGenres.sorted().joinToString(","))
        append("_types=").append(filters.types.sorted().joinToString(","))
        append("_statuses=").append(filters.statuses.sorted().joinToString(","))
        append("_from=").append(filters.fromYear ?: "")
        append("_to=").append(filters.toYear ?: "")
        append("_seasons=").append(filters.seasons.sorted().joinToString(","))
        append("_age=").append(filters.ageRatings.sorted().joinToString(","))
        append("_sort=").append(filters.sort.name)
        append("_forward=").append(filters.sortForward)
        append("_limit=").append(limit)
        append("_offset=").append(offset)
    }
}
