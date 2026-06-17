package su.afk.yummy.tv.data.search.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.storage.search.SearchStorageStore
import su.afk.yummy.tv.core.storage.search.isFresh
import su.afk.yummy.tv.data.search.dto.YaniSearchCatalogDto
import su.afk.yummy.tv.data.search.dto.YaniSearchGenresDto
import su.afk.yummy.tv.data.search.mapper.toSearchAnimeType
import su.afk.yummy.tv.data.search.mapper.toSearchGenre
import su.afk.yummy.tv.data.search.mapper.toSearchGenreGroup
import su.afk.yummy.tv.data.search.mapper.toSearchItem
import su.afk.yummy.tv.data.search.network.YaniSearchApi
import su.afk.yummy.tv.data.search.storage.mapper.toSearchFilterOptionsCache
import su.afk.yummy.tv.data.search.storage.mapper.toSearchPageCache
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchPage
import su.afk.yummy.tv.domain.search.repository.SearchRepository
import su.afk.yummy.tv.data.search.storage.mapper.toSearchFilterOptions as toStoredSearchFilterOptions
import su.afk.yummy.tv.data.search.storage.mapper.toSearchPage as toStoredSearchPage

private const val SEARCH_FILTER_OPTIONS_TTL_MS = 24 * 60 * 60 * 1000L
private const val SEARCH_RESULTS_TTL_MS = 10 * 60 * 1000L
private const val SEARCH_RESULTS_CACHE_RETENTION_MS = 24 * 60 * 60 * 1000L

private data class YaniSearchFilterOptionsDto(
    val genres: YaniSearchGenresDto = YaniSearchGenresDto(),
    val catalog: YaniSearchCatalogDto = YaniSearchCatalogDto(),
)

class YaniSearchRepository(
    private val api: YaniSearchApi,
    private val searchStorage: SearchStorageStore,
    private val settingsStore: SettingsStore,
) : SearchRepository {
    override suspend fun search(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): SearchPage = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val pageKey = searchCacheKey(query, filters, limit, offset, language)
        val stored = searchStorage.getPage(pageKey)
        if (stored?.isFresh(SEARCH_RESULTS_TTL_MS) == true) {
            return@withContext stored.toStoredSearchPage()
        }

        try {
            fetchSearchPage(
                query = query,
                filters = filters,
                limit = limit,
                offset = offset,
                pageKey = pageKey,
                language = languageCode,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toStoredSearchPage()
                ?: throw error
        }
    }

    override suspend fun getFilterOptions(): SearchFilterOptions = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = searchStorage.getFilterOptions(languageCode)
        if (stored?.isFresh(SEARCH_FILTER_OPTIONS_TTL_MS) == true) {
            return@withContext stored.toStoredSearchFilterOptions()
        }

        try {
            fetchFilterOptions(languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toStoredSearchFilterOptions()
                ?: throw error
        }
    }

    private suspend fun fetchSearchPage(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
        pageKey: String,
        language: String,
    ): SearchPage {
        val response = api.search(query, filters, limit, offset)
        val items = response.mapNotNull { it.toSearchItem() }
        val cachedAt = System.currentTimeMillis()
        searchStorage.savePage(
            items.toSearchPageCache(
                pageKey = pageKey,
                language = language,
                limit = limit,
                offset = offset,
                responseSize = response.size,
                cachedAt = cachedAt,
            ),
            prunePagesCachedBefore = cachedAt - SEARCH_RESULTS_CACHE_RETENTION_MS,
        )
        return items.toSearchPage(limit, offset, response.size)
    }

    private suspend fun fetchFilterOptions(language: String): SearchFilterOptions {
        val response = fetchFilterOptionsDto()
        val options = response.toSearchFilterOptions()
        searchStorage.saveFilterOptions(
            options.toSearchFilterOptionsCache(
                language = language,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return options
    }

    private suspend fun fetchFilterOptionsDto(): YaniSearchFilterOptionsDto =
        coroutineScope {
            val genres = async { api.getGenres() }
            val catalog = async { api.getCatalog() }
            YaniSearchFilterOptionsDto(
                genres = genres.await(),
                catalog = catalog.await(),
            )
        }

    private fun YaniSearchFilterOptionsDto.toSearchFilterOptions(): SearchFilterOptions =
        SearchFilterOptions(
            genreGroups = genres.groups.mapNotNull { it.toSearchGenreGroup() },
            genres = genres.genres.mapNotNull { it.toSearchGenre() },
            types = catalog.types.mapNotNull { it.toSearchAnimeType() },
        )

    private fun List<SearchItem>.toSearchPage(
        limit: Int,
        offset: Int,
        responseSize: Int,
    ): SearchPage =
        SearchPage(
            items = this,
            nextOffset = offset + responseSize,
            canLoadMore = responseSize >= limit,
        )

    private fun searchCacheKey(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
        language: YaniContentLanguage,
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
    }.withYaniContentLanguage(language)
}
