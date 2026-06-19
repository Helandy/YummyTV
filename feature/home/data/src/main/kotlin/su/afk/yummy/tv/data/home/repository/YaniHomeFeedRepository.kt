package su.afk.yummy.tv.data.home.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.home.HomeFeedStore
import su.afk.yummy.tv.core.storage.home.isFresh
import su.afk.yummy.tv.core.storage.watchprogress.ContinueWatchingMerge
import su.afk.yummy.tv.core.storage.watchprogress.RemoteContinueWatchingStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniVideoDto
import su.afk.yummy.tv.data.home.mapper.toHomeFeed
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.data.home.storage.mapper.toHomeContinueWatchingItem
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeedCache
import su.afk.yummy.tv.data.home.storage.mapper.toWatchProgressEntry
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.data.home.storage.mapper.toHomeFeed as toStoredHomeFeed

private const val FEED_TTL_MS = 60 * 1000L
private const val FEED_CACHE_SIGNATURE_VERSION = "cw5"
private const val TAG = "YaniHomeFeed"

class YaniHomeFeedRepository(
    private val api: YaniHomeApi,
    private val homeFeedStore: HomeFeedStore,
    private val stringProvider: StringProvider,
    private val settingsStore: SettingsStore,
    private val watchProgressStore: WatchProgressStore,
    private val remoteContinueWatchingStore: RemoteContinueWatchingStore,
    private val continueWatchingEnricher: ContinueWatchingEnricher,
) : HomeFeedRepository {

    private val remoteContinueWatchingItems =
        MutableStateFlow<List<HomeContinueWatchingItem>>(emptyList())

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun getCachedHomeFeed(): HomeFeed? = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.yaniContentLanguage.first().apiCode
        val accountKey = continueWatchingAccountKey()
        val watchSignature = feedCacheSignature(accountKey)
        val displayWatchEntries = displayWatchEntries()
        val suppressionTimestamps = watchProgressStore.continueWatchingSuppressionTimestamps()
        val watchedEntries = watchProgressStore.watchedProgress()
        val storedFeed = homeFeedStore.getFeed(languageCode, watchSignature)
            ?.toStoredHomeFeed(stringProvider)
            ?.withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
            ?: return@withContext null
        syncRemoteContinueWatching(accountKey, languageCode, storedFeed.continueWatchingItems)
        val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
        val remoteFeed = storedFeed
            .withMergedContinueWatching(remoteWatchEntries, localEntries = emptyList())
            .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
        updateRemoteContinueWatching(remoteFeed)
        remoteFeed.withMergedContinueWatching(
            remoteEntries = emptyList(),
            localEntries = displayWatchEntries,
        )
    }

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    override suspend fun removeCachedContinueWatching(animeId: Int) {
        withContext(Dispatchers.IO) {
            watchProgressStore.suppressContinueWatching(animeId)
            homeFeedStore.deleteContinueWatchingByAnimeId(animeId)
            remoteContinueWatchingStore.deleteByAnimeId(animeId)
            remoteContinueWatchingItems.value =
                remoteContinueWatchingItems.value.filterNot { it.animeId == animeId }
        }
    }

    override suspend fun getContinueWatchingVideoIds(animeId: Int): List<Int> =
        withContext(Dispatchers.IO) {
            (
                    remoteContinueWatchingItems.value.filter { it.animeId == animeId }
                        .map { it.videoId } +
                            watchProgressStore.continueWatching()
                                .filter { it.animeId == animeId }
                                .map { it.videoId }
                    )
                .filter { it > 0 }
                .distinct()
        }

    override fun observeContinueWatching(): Flow<List<HomeContinueWatchingItem>> =
        combine(
            remoteContinueWatchingItems,
            watchProgressStore.observeContinueWatching(),
            watchProgressStore.observeContinueWatchingSuppressionTimestamps(),
            watchProgressStore.observeWatchedProgress(),
        ) { feedItems, localEntries, suppressionTimestamps, watchedEntries ->
            mergeContinueWatchingItems(
                feedItems = feedItems.filterDisplayableContinueWatching(
                    suppressionTimestamps = suppressionTimestamps,
                    watchedEntries = watchedEntries,
                ),
                localEntries = localEntries,
            )
        }.distinctUntilChanged()

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val accountKey = continueWatchingAccountKey()
        val displayWatchEntries = displayWatchEntries()
        val suppressionTimestamps = watchProgressStore.continueWatchingSuppressionTimestamps()
        val watchedEntries = watchProgressStore.watchedProgress()
        val watchSignature = feedCacheSignature(accountKey)
        val stored = homeFeedStore.getFeed(languageCode, watchSignature)
        if (!forceRefresh && stored?.isFresh(FEED_TTL_MS) == true) {
            val storedFeed = stored
                .toStoredHomeFeed(stringProvider)
                .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
            syncRemoteContinueWatching(accountKey, languageCode, storedFeed.continueWatchingItems)
            val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
            val remoteFeed = storedFeed
                .withMergedContinueWatching(remoteWatchEntries, localEntries = emptyList())
                .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
            updateRemoteContinueWatching(remoteFeed)
            return@withContext remoteFeed.withMergedContinueWatching(
                remoteEntries = emptyList(),
                localEntries = displayWatchEntries,
            )
        }

        try {
            fetchHomeFeed(
                accountKey = accountKey,
                languageCode = languageCode,
                watchSignature = watchSignature,
                displayWatchEntries = displayWatchEntries,
                suppressionTimestamps = suppressionTimestamps,
                watchedEntries = watchedEntries,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val storedFeed = stored?.toStoredHomeFeed(stringProvider)
                ?.withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
                ?: throw error
            syncRemoteContinueWatching(accountKey, languageCode, storedFeed.continueWatchingItems)
            val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
            val remoteFeed = storedFeed
                .withMergedContinueWatching(remoteWatchEntries, localEntries = emptyList())
                .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
            updateRemoteContinueWatching(remoteFeed)
            remoteFeed.withMergedContinueWatching(
                remoteEntries = emptyList(),
                localEntries = displayWatchEntries,
            )
        }
    }

    private suspend fun fetchHomeFeed(
        accountKey: String,
        languageCode: String,
        watchSignature: String,
        displayWatchEntries: List<WatchProgressEntry>,
        suppressionTimestamps: Map<Int, Long>,
        watchedEntries: List<WatchProgressEntry>,
    ): HomeFeed {
        Log.i(
            TAG,
            "Fetch feed language=$languageCode watchSignature=$watchSignature",
        )
        val dto = api.getFeed()
        Log.i(TAG, "Feed dto ${dto.summaryForLog()}")
        val rawMappedFeed = dto.toHomeFeed(stringProvider, displayWatchEntries)
        syncRemoteContinueWatching(accountKey, languageCode, rawMappedFeed.continueWatchingItems)
        val mappedFeed = rawMappedFeed
            .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
        val enrichedItems = continueWatchingEnricher.enrich(
            items = mappedFeed.continueWatchingItems,
            watchEntries = displayWatchEntries,
        )
        val rawEnrichedFeed = mappedFeed.copy(continueWatchingItems = enrichedItems)
        syncRemoteContinueWatching(accountKey, languageCode, rawEnrichedFeed.continueWatchingItems)
        val enrichedFeed = rawEnrichedFeed
            .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
        val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
        val remoteFeed = enrichedFeed
            .withMergedContinueWatching(remoteWatchEntries, localEntries = emptyList())
            .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
        val feed = remoteFeed
            .withMergedContinueWatching(
                remoteEntries = emptyList(),
                localEntries = displayWatchEntries,
            )
            .withoutHiddenContinueWatching(suppressionTimestamps, watchedEntries)
        Log.i(
            TAG,
            "Feed mapped ${feed.summaryForLog()} enriched=" +
                    "${mappedFeed.continueWatchingItems.enrichedCount(enrichedItems)} " +
                    "continueSamples=${enrichedItems.summaryForLog()}",
        )
        homeFeedStore.saveFeed(
            remoteFeed.toHomeFeedCache(
                language = languageCode,
                watchSignature = watchSignature,
                cachedAt = System.currentTimeMillis(),
            )
        )
        updateRemoteContinueWatching(remoteFeed)
        return feed
    }

    private fun updateRemoteContinueWatching(feed: HomeFeed) {
        remoteContinueWatchingItems.value = feed.continueWatchingItems
    }

    private suspend fun displayWatchEntries(): List<WatchProgressEntry> =
        watchProgressStore.continueWatching()

    private suspend fun remoteWatchEntries(
        accountKey: String,
        languageCode: String,
    ): List<WatchProgressEntry> =
        remoteContinueWatchingStore.get(accountKey, languageCode)

    private suspend fun syncRemoteContinueWatching(
        accountKey: String,
        languageCode: String,
        items: List<HomeContinueWatchingItem>,
    ) {
        remoteContinueWatchingStore.saveRemoteContinueWatching(
            accountKey = accountKey,
            language = languageCode,
            entries = items.map { it.toWatchProgressEntry() },
        )
    }

    private suspend fun continueWatchingAccountKey(): String {
        val userId = settingsStore.yaniUserId.first()
        return if (userId > 0) "user:$userId" else "anon"
    }

    private fun feedCacheSignature(accountKey: String): String {
        return "$FEED_CACHE_SIGNATURE_VERSION:$accountKey"
    }

    private fun HomeFeed.withMergedContinueWatching(
        remoteEntries: List<WatchProgressEntry>,
        localEntries: List<WatchProgressEntry>,
    ): HomeFeed = copy(
        continueWatchingItems = mergeContinueWatchingItems(
            feedItems = remoteEntries.map { it.toHomeContinueWatchingItem() } + continueWatchingItems,
            localEntries = localEntries,
        )
    )

    private fun HomeFeed.withoutHiddenContinueWatching(
        suppressionTimestamps: Map<Int, Long>,
        watchedEntries: List<WatchProgressEntry>,
    ): HomeFeed = copy(
        continueWatchingItems = continueWatchingItems.filterDisplayableContinueWatching(
            suppressionTimestamps = suppressionTimestamps,
            watchedEntries = watchedEntries,
        )
    )

    private fun List<HomeContinueWatchingItem>.filterDisplayableContinueWatching(
        suppressionTimestamps: Map<Int, Long>,
        watchedEntries: List<WatchProgressEntry>,
    ): List<HomeContinueWatchingItem> {
        val watchedCandidates = watchedEntries +
                filter { item ->
                    WatchProgressStore.isWatchedProgress(item.positionMs, item.durationMs)
                }.map { it.toWatchProgressEntry() }
        return filterNot { item ->
            val entry = item.toWatchProgressEntry()
            val isSuppressed =
                suppressionTimestamps[item.animeId]?.let { it >= item.updatedAt } == true
            val isHiddenByWatched = watchedCandidates.any { watched ->
                watched.animeId == entry.animeId &&
                        watched.updatedAt >= entry.updatedAt &&
                        !ContinueWatchingMerge.isFurtherThan(entry, watched)
            }
            WatchProgressStore.isWatchedProgressEntry(entry) || isSuppressed || isHiddenByWatched
        }
    }

    private fun mergeContinueWatchingItems(
        feedItems: List<HomeContinueWatchingItem>,
        localEntries: List<WatchProgressEntry>,
    ): List<HomeContinueWatchingItem> {
        val result = linkedMapOf<Int, HomeContinueWatchingItem>()
        feedItems.forEach { item ->
            if (item.animeId <= 0) return@forEach
            val current = result[item.animeId]
            if (current == null || item.updatedAt > current.updatedAt) {
                result[item.animeId] = item
            }
        }

        ContinueWatchingMerge.bestByAnime(localEntries).forEach { local ->
            val current = result[local.animeId]
            if (current == null || local.updatedAt > current.updatedAt) {
                result[local.animeId] = local
                    .toHomeContinueWatchingItem()
                    .withFallbackMetadata(current)
            }
        }

        return result.values.sortedByDescending { it.updatedAt }
    }

    private fun HomeContinueWatchingItem.withFallbackMetadata(
        fallback: HomeContinueWatchingItem?,
    ): HomeContinueWatchingItem =
        if (fallback == null) {
            this
        } else {
            copy(
                animeTitle = animeTitle.ifBlank { fallback.animeTitle },
                description = description.ifBlank { fallback.description },
                poster = poster ?: fallback.poster,
                videoId = videoId.takeIf { it > 0 } ?: fallback.videoId,
                episode = episode.ifBlank { fallback.episode },
                episodeUrl = episodeUrl.ifBlank { fallback.episodeUrl },
                durationMs = durationMs.takeIf { it > 0L } ?: fallback.durationMs,
                playerName = playerName.ifBlank { fallback.playerName },
                dubbing = dubbing.ifBlank { fallback.dubbing },
                screenshotUrl = screenshotUrl.ifBlank { fallback.screenshotUrl },
            )
        }

    private fun YaniFeedDto.summaryForLog(): String {
        val data = response
        return buildString {
            append("announcements=${data.announcements.size}")
            append(" topCarousel=${data.topCarousel.items.size}")
            append(" new=${data.new.size}")
            append(" recommends=${data.recommends.size}")
            append(" lastWatches=${data.lastWatches.size}")
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

    private fun List<HomeContinueWatchingItem>.enrichedCount(
        enriched: List<HomeContinueWatchingItem>,
    ): Int =
        zip(enriched).count { (before, after) ->
            before.videoId != after.videoId ||
                    before.episodeUrl != after.episodeUrl ||
                    before.durationMs != after.durationMs ||
                    before.screenshotUrl != after.screenshotUrl ||
                    before.playerName != after.playerName ||
                    before.dubbing != after.dubbing
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
