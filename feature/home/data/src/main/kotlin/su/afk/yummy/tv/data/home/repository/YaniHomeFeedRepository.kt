package su.afk.yummy.tv.data.home.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.home.HOME_FEED_GENERIC_WATCH_SIGNATURE
import su.afk.yummy.tv.core.storage.home.HomeFeedStore
import su.afk.yummy.tv.core.storage.home.isFresh
import su.afk.yummy.tv.core.storage.watchprogress.ContinueWatchingMerge
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

private const val FEED_TTL_MS = 5 * 60 * 1000L
private const val FEED_WATCH_LIMIT = 8
private const val TAG = "YaniHomeFeed"

class YaniHomeFeedRepository(
    private val api: YaniHomeApi,
    private val homeFeedStore: HomeFeedStore,
    private val stringProvider: StringProvider,
    private val settingsStore: SettingsStore,
    private val watchProgressStore: WatchProgressStore,
    private val continueWatchingEnricher: ContinueWatchingEnricher,
) : HomeFeedRepository {

    override suspend fun getHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = false)

    override suspend fun getCachedHomeFeed(): HomeFeed? = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.yaniContentLanguage.first().apiCode
        val watchSignature = feedWatchEntries()
            .map { it.toFeedWatchParam() }
            .joinToString("|")
        val displayWatchEntries = displayWatchEntries()
        val stored = homeFeedStore.getFeed(languageCode, watchSignature)
        stored?.toHomeFeed(stringProvider)?.withMergedContinueWatching(displayWatchEntries)
            ?: if (watchSignature == HOME_FEED_GENERIC_WATCH_SIGNATURE) {
                null
            } else {
                homeFeedStore
                    .getFeed(languageCode, HOME_FEED_GENERIC_WATCH_SIGNATURE)
                    ?.toHomeFeed(stringProvider)
                    ?.withMergedContinueWatching(displayWatchEntries)
            }
    }

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    override suspend fun removeCachedContinueWatching(animeId: Int) {
        withContext(Dispatchers.IO) {
            homeFeedStore.deleteContinueWatchingByAnimeId(animeId)
        }
    }

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val feedWatchEntries = feedWatchEntries()
        val displayWatchEntries = displayWatchEntries()
        val watches = feedWatchEntries.map { it.toFeedWatchParam() }
        val watchSignature = watches.joinToString("|")
        val stored = homeFeedStore.getFeed(languageCode, watchSignature)
        if (!forceRefresh && stored?.isFresh(FEED_TTL_MS) == true) {
            return@withContext stored
                .toHomeFeed(stringProvider)
                .withMergedContinueWatching(displayWatchEntries)
        }
        val genericStored = if (watchSignature == HOME_FEED_GENERIC_WATCH_SIGNATURE) {
            stored
        } else {
            homeFeedStore.getFeed(languageCode, HOME_FEED_GENERIC_WATCH_SIGNATURE)
        }

        try {
            fetchHomeFeed(
                languageCode = languageCode,
                watchSignature = watchSignature,
                watches = watches,
                displayWatchEntries = displayWatchEntries,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toHomeFeed(stringProvider)?.withMergedContinueWatching(displayWatchEntries)
                ?: genericStored?.toHomeFeed(stringProvider)
                    ?.withMergedContinueWatching(displayWatchEntries)
                ?: throw error
        }
    }

    private suspend fun fetchHomeFeed(
        languageCode: String,
        watchSignature: String,
        watches: List<String>,
        displayWatchEntries: List<WatchProgressEntry>,
    ): HomeFeed {
        Log.i(
            TAG,
            "Fetch feed language=$languageCode watchSignature=$watchSignature " +
                    "watches=${watches.joinToString()}",
        )
        val dto = api.getFeed(watches)
        Log.i(TAG, "Feed dto ${dto.summaryForLog()}")
        val mappedFeed = dto.toHomeFeed(stringProvider, displayWatchEntries)
        val enrichedItems = continueWatchingEnricher.enrich(
            items = mappedFeed.continueWatchingItems,
            watchEntries = displayWatchEntries,
        )
        val enrichedFeed = mappedFeed.copy(continueWatchingItems = enrichedItems)
        val feed = enrichedFeed.copy(
            continueWatchingItems = mergeContinueWatchingItems(
                feedItems = enrichedItems,
                localEntries = displayWatchEntries,
            )
        )
        Log.i(
            TAG,
            "Feed mapped ${feed.summaryForLog()} enriched=" +
                    "${mappedFeed.continueWatchingItems.enrichedCount(enrichedItems)} " +
                    "continueSamples=${enrichedItems.summaryForLog()}",
        )
        homeFeedStore.saveFeed(
            enrichedFeed.toHomeFeedCache(
                language = languageCode,
                watchSignature = watchSignature,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return feed
    }

    private suspend fun feedWatchEntries(): List<WatchProgressEntry> =
        watchProgressStore
            .latestMeaningfulVideoProgress(FEED_WATCH_LIMIT)

    private suspend fun displayWatchEntries(): List<WatchProgressEntry> =
        watchProgressStore.continueWatching()

    private fun WatchProgressEntry.toFeedWatchParam(): String =
        "$videoId:${positionMs / 1000L}:${updatedAt / 1000L}"

    private fun HomeFeed.withMergedContinueWatching(
        localEntries: List<WatchProgressEntry>,
    ): HomeFeed = copy(
        continueWatchingItems = mergeContinueWatchingItems(
            feedItems = continueWatchingItems,
            localEntries = localEntries,
        )
    )

    private fun mergeContinueWatchingItems(
        feedItems: List<HomeContinueWatchingItem>,
        localEntries: List<WatchProgressEntry>,
    ): List<HomeContinueWatchingItem> {
        val result = linkedMapOf<Int, HomeContinueWatchingItem>()
        feedItems.forEach { item ->
            if (item.animeId <= 0) return@forEach
            val current = result[item.animeId]
            if (current == null ||
                ContinueWatchingMerge.isFurtherThan(
                    entry = item.toWatchProgressEntry(),
                    other = current.toWatchProgressEntry(),
                )
            ) {
                result[item.animeId] = item
            }
        }

        val localOnly = mutableListOf<HomeContinueWatchingItem>()
        ContinueWatchingMerge.bestByAnime(localEntries).forEach { local ->
            val current = result[local.animeId]
            val localItem = local.toHomeContinueWatchingItem()
            if (current == null) {
                localOnly += localItem
            } else if (
                ContinueWatchingMerge.isFurtherThan(
                    entry = local,
                    other = current.toWatchProgressEntry(),
                )
            ) {
                result[local.animeId] = localItem
            }
        }

        return result.values.toList() + localOnly.sortedByDescending { it.updatedAt }
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
