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
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.data.home.storage.mapper.toHomeContinueWatchingItem
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeedCache
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeed as toStoredHomeFeed

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

    private class LocalFeedContext(
        val languageCode: String,
        val watchEntries: List<WatchProgressEntry>,
        val hiddenIds: Set<Int>,
    )

    // Применяется одинаково к результату из кэша, из сети и к fallback при ошибке: "продолжить
    // просмотр" всегда пересчитывается из актуального локального прогресса, а не из момента
    // кэширования фида, а скрытые пользователем рекомендации отфильтровываются до ближайшего
    // пересчёта рекомендаций на бэкенде.
    private fun HomeFeed.withLocalOverrides(
        localEntries: List<WatchProgressEntry>,
        hiddenIds: Set<Int>,
    ): HomeFeed = copy(
        continueWatchingItems = localContinueWatchingItems(localEntries),
        sections = if (hiddenIds.isEmpty()) {
            sections
        } else {
            sections.map { section ->
                if (section.type == HomeFeedSectionType.RECOMMENDATIONS) {
                    section.copy(items = section.items.filterNot { it.id in hiddenIds })
                } else {
                    section
                }
            }
        },
    )

    private fun feedCacheSignature(): String = FEED_CACHE_SIGNATURE_VERSION

    private fun localContinueWatchingItems(
        entries: List<WatchProgressEntry>,
    ): List<HomeContinueWatchingItem> =
        entries
            .filter { it.animeId > 0 }
            .groupBy { it.animeId }
            .values
            .mapNotNull { group ->
                group.maxWithOrNull(
                    compareBy<WatchProgressEntry> { it.updatedAt }
                        .thenBy { it.positionMs }
                        .thenBy { it.videoId }
                        .thenBy { it.episode }
                )
            }
            .sortedByDescending { it.updatedAt }
            .map { it.toHomeContinueWatchingItem() }
}
