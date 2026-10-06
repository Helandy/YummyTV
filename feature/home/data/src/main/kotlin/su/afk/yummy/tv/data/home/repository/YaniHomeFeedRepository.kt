package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.home.HomeFeedCache
import su.afk.yummy.tv.core.storage.home.HomeFeedStorage
import su.afk.yummy.tv.core.storage.home.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.utils.episode.episodeNumberOrNull
import su.afk.yummy.tv.data.home.mapper.LocalFeedContext
import su.afk.yummy.tv.data.home.mapper.localContinueWatchingItems
import su.afk.yummy.tv.data.home.mapper.withLocalOverrides
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeed as toStoredHomeFeed
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeedCache
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository

private const val FEED_TTL_MS = 60 * 1000L
private const val FEED_CACHE_SIGNATURE_VERSION = "cw-local1"
private const val TAG = "YaniHomeFeed"

class YaniHomeFeedRepository(
    private val api: YaniHomeApi,
    private val homeFeedStore: HomeFeedStorage,
    private val stringProvider: StringProvider,
    private val settingsStore: YaniAccountSettingsStore,
    private val watchProgressStore: WatchProgressStorage,
    private val analyticsTracker: AnalyticsTracker,
) : HomeFeedRepository {

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun getCachedHomeFeed(): HomeFeed? = withContext(Dispatchers.IO) {
        val local = readLocalFeedContext()
        homeFeedStore.getFeed(local.languageCode, feedCacheSignature())
            ?.toStoredHomeFeed(stringProvider)
            ?.withLocalOverrides(local.watchEntries, local.hiddenIds)
    }

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    override suspend fun removeCachedContinueWatching(animeId: Int) {
        withContext(Dispatchers.IO) {
            watchProgressStore.suppressContinueWatchingDisplay(animeId)
            homeFeedStore.deleteContinueWatchingByAnimeId(animeId)
        }
    }

    override suspend fun getContinueWatchingVideoIds(animeId: Int): List<Int> =
        withContext(Dispatchers.IO) {
            watchProgressStore.continueWatching()
                .filter { it.animeId == animeId }
                .map { it.videoId }
                .filter { it > 0 }
                .distinct()
        }

    override suspend fun migrateContinueWatchingProgress(
        migration: ContinueWatchingProgressMigration,
    ) = withContext(Dispatchers.IO) {
        watchProgressStore.save(
            animeId = migration.animeId,
            episode = migration.episode,
            videoId = migration.videoId,
            episodeUrl = migration.episodeUrl,
            positionMs = migration.positionMs,
            durationMs = migration.durationMs,
            animeTitle = migration.animeTitle,
            posterUrl = migration.posterUrl,
            playerName = migration.playerName,
            dubbing = migration.dubbing,
            screenshotUrl = migration.screenshotUrl,
        )
        watchProgressStore.delete(migration.animeId, migration.previousEpisode)
    }

    override fun observeWatchedEpisodes(): Flow<Map<Int, Set<Int>>> =
        watchProgressStore.observeWatchedProgress()
            .map { entries ->
                entries
                    .groupBy { it.animeId }
                    .mapValues { (_, items) ->
                        items.mapNotNullTo(mutableSetOf()) { entry ->
                            // Спецвыпуски вроде «7.5» — не серия 7, иначе она ложно считалась бы
                            // просмотренной.
                            entry.episode.episodeNumberOrNull()
                                ?.takeIf { it % 1.0 == 0.0 }
                                ?.toInt()
                        }
                    }
            }
            .distinctUntilChanged()

    override fun observeContinueWatching(): Flow<List<HomeContinueWatchingItem>> =
        watchProgressStore.observeContinueWatching()
            .map(::localContinueWatchingItems)
            .distinctUntilChanged()

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        // Единый снимок на весь вызов: используется во всех трёх ветках (свежий кэш, сеть,
        // fallback при ошибке), чтобы не пересчитывать его отдельно для сетевой ветки.
        val local = readLocalFeedContext()
        val watchSignature = feedCacheSignature()
        offlineFirstCache(
            forceRefresh = forceRefresh,
            read = { homeFeedStore.getFeed(local.languageCode, watchSignature) },
            isFresh = { it.isFresh(FEED_TTL_MS) },
            toDomain = { it.toStoredHomeFeed(stringProvider) },
            fetchAndSave = { fetchHomeFeed(local.languageCode, watchSignature) },
            transform = { it.withLocalOverrides(local.watchEntries, local.hiddenIds) },
        )
    }

    private suspend fun fetchHomeFeed(
        languageCode: String,
        watchSignature: String,
    ): HomeFeedCache {
        analyticsTracker.log(TAG) { "Fetch feed language=$languageCode watchSignature=$watchSignature" }
        val dto = api.getFeed()
        analyticsTracker.log(TAG) { "Feed dto ${dto.summaryForLog()}" }
        val cache = dto.toHomeFeedCache(
            language = languageCode,
            watchSignature = watchSignature,
            cachedAt = System.currentTimeMillis(),
        )
        homeFeedStore.saveFeed(cache)
        analyticsTracker.log(TAG) {
            val feed = cache.toStoredHomeFeed(stringProvider)
            "Feed mapped ${feed.summaryForLog()} " +
                    "continueSamples=${feed.continueWatchingItems.summaryForLog()}"
        }
        return cache
    }

    /** Язык, локальный прогресс (Room) и скрытые рекомендации независимы — читаются параллельно. */
    private suspend fun readLocalFeedContext(): LocalFeedContext = coroutineScope {
        val languageCode = async { settingsStore.currentLanguageCode() }
        val watchEntries = async { watchProgressStore.continueWatching() }
        val hiddenIds = async { settingsStore.hiddenRecommendationIds.first() }
        LocalFeedContext(
            languageCode = languageCode.await(),
            watchEntries = watchEntries.await(),
            hiddenIds = hiddenIds.await(),
        )
    }

    private fun feedCacheSignature(): String = FEED_CACHE_SIGNATURE_VERSION
}
