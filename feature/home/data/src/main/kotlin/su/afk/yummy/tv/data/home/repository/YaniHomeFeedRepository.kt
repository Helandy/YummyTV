package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.home.HomeFeedStore
import su.afk.yummy.tv.core.storage.home.isFresh
import su.afk.yummy.tv.data.home.mapper.toHomeFeed
import su.afk.yummy.tv.data.home.mapper.toHomeFeedCache
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository

private const val FEED_TTL_MS = 60 * 60 * 1000L

class YaniHomeFeedRepository(
    private val api: YaniHomeApi,
    private val homeFeedStore: HomeFeedStore,
    private val stringProvider: StringProvider,
    private val settingsStore: SettingsStore,
) : HomeFeedRepository {

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = homeFeedStore.getFeed(languageCode)
        if (!forceRefresh && stored?.isFresh(FEED_TTL_MS) == true) {
            return@withContext stored.toHomeFeed(stringProvider)
        }

        try {
            fetchHomeFeed(languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toHomeFeed(stringProvider)
                ?: throw error
        }
    }

    private suspend fun fetchHomeFeed(languageCode: String): HomeFeed {
        val dto = api.getFeed()
        val feed = dto.toHomeFeed(stringProvider)
        homeFeedStore.saveFeed(
            feed.toHomeFeedCache(
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return feed
    }
}
