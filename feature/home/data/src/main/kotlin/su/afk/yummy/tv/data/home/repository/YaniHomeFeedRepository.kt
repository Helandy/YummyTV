package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.logger.AppLogger
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.home.HomeFeedStore
import su.afk.yummy.tv.core.storage.home.isFresh
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniVideoDto
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
    private val homeFeedStore: HomeFeedStore,
    private val stringProvider: StringProvider,
    private val settingsStore: SettingsStore,
    private val watchProgressStore: WatchProgressStore,
) : HomeFeedRepository {

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun getCachedHomeFeed(): HomeFeed? = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.yaniContentLanguage.first().apiCode
        val watchSignature = feedCacheSignature()
        val displayWatchEntries = displayWatchEntries()
        val hiddenIds = hiddenRecommendationIds()
        homeFeedStore.getFeed(languageCode, watchSignature)
            ?.toStoredHomeFeed(stringProvider)
            ?.withLocalContinueWatching(displayWatchEntries)
            ?.withoutHiddenRecommendations(hiddenIds)
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
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val displayWatchEntries = displayWatchEntries()
        val watchSignature = feedCacheSignature()
        val stored = homeFeedStore.getFeed(languageCode, watchSignature)
        val hiddenIds = hiddenRecommendationIds()
        if (!forceRefresh && stored?.isFresh(FEED_TTL_MS) == true) {
            return@withContext stored
                .toStoredHomeFeed(stringProvider)
                .withLocalContinueWatching(displayWatchEntries)
                .withoutHiddenRecommendations(hiddenIds)
        }

        try {
            fetchHomeFeed(
                languageCode = languageCode,
                watchSignature = watchSignature,
                displayWatchEntries = displayWatchEntries,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toStoredHomeFeed(stringProvider)
                ?.withLocalContinueWatching(displayWatchEntries)
                ?.withoutHiddenRecommendations(hiddenIds)
                ?: throw error
        }
    }

    private suspend fun fetchHomeFeed(
        languageCode: String,
        watchSignature: String,
        displayWatchEntries: List<WatchProgressEntry>,
    ): HomeFeed {
        AppLogger.i(TAG) { "Fetch feed language=$languageCode watchSignature=$watchSignature" }
        val dto = api.getFeed()
        AppLogger.i(TAG) { "Feed dto ${dto.summaryForLog()}" }
        val cache = dto.toHomeFeedCache(
            language = languageCode,
            watchSignature = watchSignature,
            cachedAt = System.currentTimeMillis(),
        )
        homeFeedStore.saveFeed(cache)
        // Возвращаем результат через тот же cache->domain маппер, что и при чтении из кэша,
        // чтобы свежая загрузка и повторное чтение всегда давали одинаковый HomeFeed.
        val feed = cache.toStoredHomeFeed(stringProvider)
            .withLocalContinueWatching(displayWatchEntries)
            .withoutHiddenRecommendations(hiddenRecommendationIds())
        AppLogger.i(TAG) {
            "Feed mapped ${feed.summaryForLog()} " +
                    "continueSamples=${feed.continueWatchingItems.summaryForLog()}"
        }
        return feed
    }

    private suspend fun displayWatchEntries(): List<WatchProgressEntry> =
        watchProgressStore.continueWatching()

    private suspend fun hiddenRecommendationIds(): Set<Int> =
        settingsStore.hiddenRecommendationIds.first()

    // Тайтлы, которые пользователь попросил не рекомендовать, бэкенд отдаёт до ближайшего
    // пересчёта рекомендаций — отсекаем их и в кэше, и в свежем ответе.
    private fun HomeFeed.withoutHiddenRecommendations(hiddenIds: Set<Int>): HomeFeed {
        if (hiddenIds.isEmpty()) return this
        return copy(
            sections = sections.map { section ->
                if (section.type == HomeFeedSectionType.RECOMMENDATIONS) {
                    section.copy(items = section.items.filterNot { it.id in hiddenIds })
                } else {
                    section
                }
            }
        )
    }

    private fun feedCacheSignature(): String = FEED_CACHE_SIGNATURE_VERSION

    private fun HomeFeed.withLocalContinueWatching(
        localEntries: List<WatchProgressEntry>,
    ): HomeFeed = copy(
        continueWatchingItems = localContinueWatchingItems(localEntries),
    )

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

    private fun YaniFeedDto.summaryForLog(): String {
        val data = response
        return buildString {
            append("announcements=${data.announcements.size}")
            append(" topCarousel=${data.topCarousel.items.size}")
            append(" new=${data.new.size}")
            append(" recommends=${data.recommends.size}")
            append(" newVideos=${data.newVideos.size}")
            append(" schedule=${data.schedule.size}")
            append(" posts=${data.posts.items.size}")
            append(" bloggerVideos=${data.blogger.videos.items.size}")
            append(" collections=${data.collections.size}")
            append(" newVideoSamples=")
            append(data.newVideos.take(LOG_SAMPLE_LIMIT).joinToString(prefix = "[", postfix = "]") {
                it.summaryForLog()
            })
        }
    }

    private fun YaniVideoDto.summaryForLog(): String =
        "video=$videoId anime=$animeId title=${title.safeForLog()} " +
                "episode=${episodeTitle.safeForLog()} dub=${dubTitle.safeForLog()} " +
                "player=${playerTitle.safeForLog()}"

    private fun HomeFeed.summaryForLog(): String =
        "continueWatching=${continueWatchingItems.size} hero=${heroItems.size} sections=" +
                sections.joinToString(prefix = "[", postfix = "]") { section ->
                    "${section.type}:${section.items.size}"
                }

    private fun List<HomeContinueWatchingItem>.summaryForLog(): String =
        take(LOG_SAMPLE_LIMIT).joinToString(prefix = "[", postfix = "]") {
            "anime=${it.animeId} episode=${it.episode.safeForLog()} video=${it.videoId} " +
                    "durationMs=${it.durationMs} screenshot=${it.screenshotSourceForLog()}"
        }

    private fun HomeContinueWatchingItem.screenshotSourceForLog(): String =
        when {
            screenshotUrl.isBlank() -> "none"
            screenshotUrl.contains("kodik", ignoreCase = true) -> "kodik"
            screenshotUrl.isLikelyImageUrl() -> "direct"
            else -> "source"
        }

    private fun String?.safeForLog(): String =
        this
            ?.lineSequence()
            ?.joinToString(" ")
            ?.take(LOG_TEXT_LIMIT)
            ?: "null"

    private fun String.isLikelyImageUrl(): Boolean =
        Regex("""\.(webp|avif|jpe?g|png)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(this)

    private companion object {
        const val LOG_SAMPLE_LIMIT = 8
        const val LOG_TEXT_LIMIT = 80
    }
}
