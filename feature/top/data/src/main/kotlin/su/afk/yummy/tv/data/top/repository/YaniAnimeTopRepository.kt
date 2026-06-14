package su.afk.yummy.tv.data.top.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.top.AnimeTopStore
import su.afk.yummy.tv.core.storage.top.isFresh
import su.afk.yummy.tv.data.top.mapper.toAnimeTopItem
import su.afk.yummy.tv.data.top.mapper.toAnimeTopPage
import su.afk.yummy.tv.data.top.mapper.toAnimeTopPageCache
import su.afk.yummy.tv.data.top.network.YaniAnimeTopApi
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.repository.AnimeTopRepository

private const val ANIME_TOP_TTL_MS = 6 * 60 * 60 * 1000L
private const val ANIME_TOP_CACHE_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L

class YaniAnimeTopRepository(
    private val api: YaniAnimeTopApi,
    private val topStore: AnimeTopStore,
    private val settingsStore: SettingsStore,
) : AnimeTopRepository {

    override suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = topStore.getPage(type.apiValue, languageCode, limit, offset)
            if (stored?.isFresh(ANIME_TOP_TTL_MS) == true) {
                return@withContext stored.toAnimeTopPage()
            }

            try {
                fetchTopAnime(type, limit, offset, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toAnimeTopPage()
                    ?: throw error
            }
        }

    private suspend fun fetchTopAnime(
        type: AnimeTopType,
        limit: Int,
        offset: Int,
        languageCode: String,
    ): AnimeTopPage {
        val response = api.getTopAnime(type, limit, offset).response
        val items = response.mapNotNull { it.toAnimeTopItem() }
        val cachedAt = System.currentTimeMillis()
        topStore.savePage(
            items.toAnimeTopPageCache(
                type = type,
                language = languageCode,
                limit = limit,
                offset = offset,
                responseSize = response.size,
                cachedAt = cachedAt,
            ),
            prunePagesCachedBefore = cachedAt - ANIME_TOP_CACHE_RETENTION_MS,
        )
        return items.toAnimeTopPage(limit, offset, response.size)
    }

    private fun List<AnimeTopItem>.toAnimeTopPage(
        limit: Int,
        offset: Int,
        responseSize: Int,
    ): AnimeTopPage =
        AnimeTopPage(
            items = this,
            nextOffset = offset + responseSize,
            canLoadMore = responseSize >= limit,
        )
}
