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
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniVideoDto
import su.afk.yummy.tv.data.home.mapper.toHomeFeed
import su.afk.yummy.tv.data.home.mapper.toHomeFeedCache
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository

private const val FEED_TTL_MS = 60 * 60 * 1000L
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

    override suspend fun refreshHomeFeed(): HomeFeed = getHomeFeed(forceRefresh = true)

    private suspend fun getHomeFeed(forceRefresh: Boolean): HomeFeed = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val watchEntries = feedWatchEntries()
        val watches = watchEntries.map { it.toFeedWatchParam() }
        val watchSignature = watches.joinToString("|")
        val stored = homeFeedStore.getFeed(languageCode, watchSignature)
        if (!forceRefresh && stored?.isFresh(FEED_TTL_MS) == true) {
            return@withContext stored.toHomeFeed(stringProvider)
        }
        val genericStored = if (watchSignature == HOME_FEED_GENERIC_WATCH_SIGNATURE) {
            stored
        } else {
            homeFeedStore.getFeed(languageCode, HOME_FEED_GENERIC_WATCH_SIGNATURE)
        }

        try {
            fetchHomeFeed(languageCode, watchSignature, watches, watchEntries)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toHomeFeed(stringProvider)
                ?: genericStored?.toHomeFeed(stringProvider)
                ?: throw error
        }
    }

    private suspend fun fetchHomeFeed(
        languageCode: String,
        watchSignature: String,
        watches: List<String>,
        watchEntries: List<WatchProgressEntry>,
    ): HomeFeed {
        Log.i(
            TAG,
            "Fetch feed language=$languageCode watchSignature=$watchSignature " +
                    "watches=${watches.joinToString()}",
        )
        val dto = api.getFeed(watches)
        Log.i(TAG, "Feed dto ${dto.summaryForLog()}")
        val mappedFeed = dto.toHomeFeed(stringProvider, watchEntries)
        val enrichedItems = continueWatchingEnricher.enrich(
            items = mappedFeed.continueWatchingItems,
            watchEntries = watchEntries,
        )
        val feed = mappedFeed.copy(continueWatchingItems = enrichedItems)
        Log.i(
            TAG,
            "Feed mapped ${feed.summaryForLog()} enriched=" +
                    "${mappedFeed.continueWatchingItems.enrichedCount(enrichedItems)} " +
                    "continueSamples=${enrichedItems.summaryForLog()}",
        )
        homeFeedStore.saveFeed(
            feed.toHomeFeedCache(
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

    private fun WatchProgressEntry.toFeedWatchParam(): String =
        "$videoId:${positionMs / 1000L}:${updatedAt / 1000L}"

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
