package su.afk.yummy.tv.data.catalog.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.model.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.network.yani.MirrorDataSkippedException
import su.afk.yummy.tv.core.network.yani.withMirrorTrace
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.core.storage.search.SearchStorage
import su.afk.yummy.tv.core.storage.search.isFresh
import su.afk.yummy.tv.data.catalog.mapper.toSearchItem
import su.afk.yummy.tv.data.catalog.network.YaniSearchApi
import su.afk.yummy.tv.data.catalog.storage.mapper.toSearchFilterOptionsCache
import su.afk.yummy.tv.data.catalog.storage.mapper.toSearchPageCache
import su.afk.yummy.tv.domain.catalog.model.CachedSearchPage
import su.afk.yummy.tv.domain.catalog.model.SearchFilterOptions
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.model.SearchPage
import su.afk.yummy.tv.domain.catalog.repository.SearchRepository
import su.afk.yummy.tv.data.catalog.storage.mapper.toSearchFilterOptions as toStoredSearchFilterOptions
import su.afk.yummy.tv.data.catalog.storage.mapper.toSearchPage as toStoredSearchPage

private const val SEARCH_FILTER_OPTIONS_TTL_MS = 24 * 60 * 60 * 1000L
private const val SEARCH_RESULTS_TTL_MS = 60 * 60 * 1000L
private const val SEARCH_RESULTS_CACHE_RETENTION_MS = 24 * 60 * 60 * 1000L
private const val MIRROR_RESULTS_TTL_MS = 5 * 60 * 1000L

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
        forceRefresh: Boolean,
    ): SearchPage = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val pageKey = searchCacheKey(query, filters, limit, offset, language)
        offlineFirstCache(
            forceRefresh = forceRefresh,
            read = { searchStorage.getPage(pageKey) },
            isFresh = { it.isFresh(SEARCH_RESULTS_TTL_MS) },
            toDomain = { it.toStoredSearchPage() },
            fetchAndSave = {
                val (response, fromMirror) = withMirrorTrace { api.search(query, filters, limit, offset) }
                val now = System.currentTimeMillis()
                // Страница из копии неполна (под фильтрами нет тайтлов без деталей): сохранённая страница yani не заменяется,
                // а пустое место заполняется ненадолго, чтобы yani опросили снова.
                if (fromMirror && searchStorage.getPage(pageKey)?.items?.isNotEmpty() == true) throw MirrorDataSkippedException()
                val cachedAt = if (fromMirror) now - SEARCH_RESULTS_TTL_MS + MIRROR_RESULTS_TTL_MS else now
                val cache = response.toSearchPageCache(
                    pageKey = pageKey,
                    language = language.apiCode,
                    limit = limit,
                    offset = offset,
                    responseSize = response.size,
                    cachedAt = cachedAt,
                )
                // Пока отвечает копия, старые страницы не вычищаются: если не станет и её, сохранённое — единственное, что есть.
                searchStorage.savePage(
                    cache,
                    prunePagesCachedBefore = if (fromMirror) 0L else now - SEARCH_RESULTS_CACHE_RETENTION_MS,
                )
                cache
            },
        )
    }

    override suspend fun getCachedSearchPage(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): CachedSearchPage? = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        searchStorage.getPage(searchCacheKey(query, filters, limit, offset, language))?.let { cache ->
            CachedSearchPage(
                page = cache.toStoredSearchPage(),
                isFresh = cache.isFresh(SEARCH_RESULTS_TTL_MS),
            )
        }
    }

    override suspend fun getFilterOptions(): SearchFilterOptions = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.currentLanguageCode()
        offlineFirstCache(
            read = { searchStorage.getFilterOptions(languageCode) },
            isFresh = { it.isFresh(SEARCH_FILTER_OPTIONS_TTL_MS) },
            toDomain = { it.toStoredSearchFilterOptions() },
            fetchAndSave = {
                val cache = api.getGenres().toSearchFilterOptionsCache(
                    language = languageCode,
                    cachedAt = System.currentTimeMillis(),
                )
                searchStorage.saveFilterOptions(cache)
                cache
            },
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
