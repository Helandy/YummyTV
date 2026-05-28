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
        val response = api.search(query, filters, limit, offset)
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
}
