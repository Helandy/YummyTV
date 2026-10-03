package su.afk.yummy.tv.data.top.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.core.storage.top.AnimeTopStorage
import su.afk.yummy.tv.core.storage.top.isFresh
import su.afk.yummy.tv.data.top.network.YaniAnimeTopApi
import su.afk.yummy.tv.data.top.storage.mapper.toAnimeTopPageCache
import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.repository.AnimeTopRepository
import su.afk.yummy.tv.data.top.storage.mapper.toAnimeTopPage as toStoredAnimeTopPage

private const val ANIME_TOP_TTL_MS = 6 * 60 * 60 * 1000L
private const val ANIME_TOP_CACHE_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L

class YaniAnimeTopRepository(
    private val api: YaniAnimeTopApi,
    private val topStore: AnimeTopStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : AnimeTopRepository {

    override suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { topStore.getPage(type.apiValue, languageCode, limit, offset) },
                isFresh = { it.isFresh(ANIME_TOP_TTL_MS) },
                toDomain = { it.toStoredAnimeTopPage() },
                fetchAndSave = {
                    val response = api.getTopAnime(type, limit, offset).response
                    val cachedAt = System.currentTimeMillis()
                    val cache = response.toAnimeTopPageCache(
                        type = type,
                        language = languageCode,
                        limit = limit,
                        offset = offset,
                        responseSize = response.size,
                        cachedAt = cachedAt,
                    )
                    topStore.savePage(
                        cache,
                        prunePagesCachedBefore = cachedAt - ANIME_TOP_CACHE_RETENTION_MS,
                    )
                    cache
                },
            )
        }
}
