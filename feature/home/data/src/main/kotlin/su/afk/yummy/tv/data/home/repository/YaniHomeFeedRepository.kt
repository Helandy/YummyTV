package su.afk.yummy.tv.data.home.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import su.afk.yummy.tv.data.home.mapper.toHomeFeedCache
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository

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

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun getCachedHomeFeed(): HomeFeed? = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.yaniContentLanguage.first().apiCode
        val accountKey = continueWatchingAccountKey()
        val watchSignature = feedCacheSignature(accountKey)
        val displayWatchEntries = displayWatchEntries()
        val suppressedAnimeIds = watchProgressStore.suppressedContinueWatchingAnimeIds()
        val storedFeed = homeFeedStore.getFeed(languageCode, watchSignature)
            ?.toHomeFeed(stringProvider)
            ?.withoutSuppressedContinueWatching(suppressedAnimeIds)
            ?: return@withContext null
        syncRemoteContinueWatching(accountKey, languageCode, storedFeed.continueWatchingItems)
        val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
        storedFeed.withMergedContinueWatching(remoteWatchEntries, displayWatchEntries)
    }

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    override suspend fun removeCachedContinueWatching(animeId: Int) {
        withContext(Dispatchers.IO) {
            homeFeedStore.deleteContinueWatchingByAnimeId(animeId)
            remoteContinueWatchingStore.deleteByAnimeId(animeId)
        }
    }

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val accountKey = continueWatchingAccountKey()
        val displayWatchEntries = displayWatchEntries()
        val suppressedAnimeIds = watchProgressStore.suppressedContinueWatchingAnimeIds()
        val watchSignature = feedCacheSignature(accountKey)
        val stored = homeFeedStore.getFeed(languageCode, watchSignature)
        if (!forceRefresh && stored?.isFresh(FEED_TTL_MS) == true) {
            val storedFeed = stored
                .toHomeFeed(stringProvider)
                .withoutSuppressedContinueWatching(suppressedAnimeIds)
            syncRemoteContinueWatching(accountKey, languageCode, storedFeed.continueWatchingItems)
            val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
            return@withContext storedFeed.withMergedContinueWatching(
                remoteWatchEntries,
                displayWatchEntries
            )
        }

        try {
            fetchHomeFeed(
                accountKey = accountKey,
                languageCode = languageCode,
                watchSignature = watchSignature,
                displayWatchEntries = displayWatchEntries,
                suppressedAnimeIds = suppressedAnimeIds,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val storedFeed = stored?.toHomeFeed(stringProvider)
                ?.withoutSuppressedContinueWatching(suppressedAnimeIds)
                ?: throw error
            syncRemoteContinueWatching(accountKey, languageCode, storedFeed.continueWatchingItems)
            val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
            storedFeed.withMergedContinueWatching(remoteWatchEntries, displayWatchEntries)
        }
    }

    private suspend fun fetchHomeFeed(
        accountKey: String,
        languageCode: String,
        watchSignature: String,
        displayWatchEntries: List<WatchProgressEntry>,
        suppressedAnimeIds: Set<Int>,
    ): HomeFeed {
        Log.i(
            TAG,
            "Fetch feed language=$languageCode watchSignature=$watchSignature",
        )
        val dto = api.getFeed()
        Log.i(TAG, "Feed dto ${dto.summaryForLog()}")
        val mappedFeed = dto
            .toHomeFeed(stringProvider, displayWatchEntries)
            .withoutSuppressedContinueWatching(suppressedAnimeIds)
        val enrichedItems = continueWatchingEnricher.enrich(
            items = mappedFeed.continueWatchingItems,
            watchEntries = displayWatchEntries,
        )
        val enrichedFeed = mappedFeed.copy(continueWatchingItems = enrichedItems)
            .withoutSuppressedContinueWatching(suppressedAnimeIds)
        syncRemoteContinueWatching(accountKey, languageCode, enrichedFeed.continueWatchingItems)
        val remoteWatchEntries = remoteWatchEntries(accountKey, languageCode)
        val feed = enrichedFeed
            .withMergedContinueWatching(remoteWatchEntries, displayWatchEntries)
            .withoutSuppressedContinueWatching(suppressedAnimeIds)
        val cacheFeed = enrichedFeed
            .withMergedContinueWatching(remoteWatchEntries, localEntries = emptyList())
            .withoutSuppressedContinueWatching(suppressedAnimeIds)
        Log.i(
            TAG,
            "Feed mapped ${feed.summaryForLog()} enriched=" +
                    "${mappedFeed.continueWatchingItems.enrichedCount(enrichedItems)} " +
                    "continueSamples=${enrichedItems.summaryForLog()}",
        )
        homeFeedStore.saveFeed(
            cacheFeed.toHomeFeedCache(
                language = languageCode,
                watchSignature = watchSignature,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return feed
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

    private fun HomeFeed.withoutSuppressedContinueWatching(
        suppressedAnimeIds: Set<Int>,
    ): HomeFeed =
        if (suppressedAnimeIds.isEmpty()) {
            this
        } else {
            copy(continueWatchingItems = continueWatchingItems.filterNot { it.animeId in suppressedAnimeIds })
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
            result[local.animeId] = local.toHomeContinueWatchingItem()
        }

        return result.values.sortedByDescending { it.updatedAt }
    }

    private fun HomeContinueWatchingItem.toWatchProgressEntry(): WatchProgressEntry =
        WatchProgressEntry(
            animeId = animeId,
            episode = episode,
            videoId = videoId,
            episodeUrl = episodeUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = updatedAt,
            animeTitle = animeTitle,
            posterUrl = poster?.bestUrl().orEmpty(),
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )

    private fun WatchProgressEntry.toHomeContinueWatchingItem(): HomeContinueWatchingItem =
        HomeContinueWatchingItem(
            animeId = animeId,
            animeTitle = animeTitle,
            description = "",
            poster = posterUrl.takeIf { it.isNotBlank() }?.let { it.toHomePoster() },
            videoId = videoId,
            episode = episode,
            episodeUrl = episodeUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = updatedAt,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )

    private fun HomePoster.bestUrl(): String? =
        mega ?: fullsize ?: big ?: medium ?: small

    private fun String.toHomePoster(): HomePoster =
        HomePoster(
            small = null,
            medium = null,
            big = null,
            fullsize = null,
            mega = this,
        )

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
