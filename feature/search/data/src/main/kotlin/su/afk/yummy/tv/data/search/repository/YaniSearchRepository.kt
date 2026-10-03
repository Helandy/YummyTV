package su.afk.yummy.tv.data.search.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.model.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.core.storage.search.SearchStorage
import su.afk.yummy.tv.core.storage.search.isFresh
import su.afk.yummy.tv.data.search.dto.YaniSearchFilterOptionsDto
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

class YaniSearchRepository(
    private val api: YaniSearchApi,
    private val searchStorage: SearchStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : SearchRepository {
    override suspend fun getRandomAnime(): SearchItem? = withContext(Dispatchers.IO) {
        api.getRandomAnime().firstNotNullOfOrNull { it.toSearchItem() }
    }

    override suspend fun search(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): SearchPage = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val pageKey = searchCacheKey(query, filters, limit, offset, language)
        offlineFirstCache(
            read = { searchStorage.getPage(pageKey) },
            isFresh = { it.isFresh(SEARCH_RESULTS_TTL_MS) },
            toDomain = { it.toStoredSearchPage() },
            fetchAndSave = {
                val response = api.search(query, filters, limit, offset)
                val cachedAt = System.currentTimeMillis()
                val cache = response.toSearchPageCache(
                    pageKey = pageKey,
                    language = language.apiCode,
                    limit = limit,
                    offset = offset,
                    responseSize = response.size,
                    cachedAt = cachedAt,
                )
                searchStorage.savePage(
                    cache,
                    prunePagesCachedBefore = cachedAt - SEARCH_RESULTS_CACHE_RETENTION_MS,
                )
                cache
            },
        )
    }

    override suspend fun getFilterOptions(): SearchFilterOptions = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.currentLanguageCode()
        offlineFirstCache(
            read = { searchStorage.getFilterOptions(languageCode) },
            isFresh = { it.isFresh(SEARCH_FILTER_OPTIONS_TTL_MS) },
            toDomain = { it.toStoredSearchFilterOptions() },
            fetchAndSave = {
                val response = fetchFilterOptionsDto()
                val cache = response.genres.toSearchFilterOptionsCache(
                    catalog = response.catalog,
                    language = languageCode,
                    cachedAt = System.currentTimeMillis(),
                )
                searchStorage.saveFilterOptions(cache)
                cache
            },
        )
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
