package su.afk.yummy.tv.data.home.repository

import su.afk.yummy.tv.core.utils.network.isLikelyImageUrl
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniVideoDto
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed

private const val LOG_SAMPLE_LIMIT = 8
private const val LOG_TEXT_LIMIT = 80

internal fun YaniFeedDto.summaryForLog(): String {
    val data = response
    return buildString {
        append("announcements=${data.announcements.size}")
        append(" topCarousel=${data.topCarousel.items.size}")
        append(" new=${data.new.size}")
        append(" recommends=${data.recommends.size}")
        append(" newVideos=${data.newVideos.size}")
        append(" posts=${data.posts.items.size}")
        append(" bloggerVideos=${data.blogger.videos.items.size}")
        append(" collections=${data.collections.size}")
        append(" newVideoSamples=")
        append(data.newVideos.take(LOG_SAMPLE_LIMIT).joinToString(prefix = "[", postfix = "]") {
            it.summaryForLog()
        })
    }
}

internal fun YaniVideoDto.summaryForLog(): String =
    "video=$videoId anime=$animeId title=${title.safeForLog()} " +
            "episode=${episodeTitle.safeForLog()} dub=${dubTitle.safeForLog()} " +
            "player=${playerTitle.safeForLog()}"

internal fun HomeFeed.summaryForLog(): String =
    "continueWatching=${continueWatchingItems.size} hero=${heroItems.size} sections=" +
            sections.joinToString(prefix = "[", postfix = "]") { section ->
                "${section.type}:${section.items.size}"
            }

internal fun List<HomeContinueWatchingItem>.summaryForLog(): String =
    take(LOG_SAMPLE_LIMIT).joinToString(prefix = "[", postfix = "]") {
        "anime=${it.animeId} episode=${it.episode.safeForLog()} video=${it.videoId} " +
                "durationMs=${it.durationMs} screenshot=${it.screenshotSourceForLog()}"
    }

internal fun HomeContinueWatchingItem.screenshotSourceForLog(): String =
    when {
        screenshotUrl.isBlank() -> "none"
        screenshotUrl.contains("kodik", ignoreCase = true) -> "kodik"
        screenshotUrl.isLikelyImageUrl() -> "direct"
        else -> "source"
    }

internal fun String?.safeForLog(): String =
    this
        ?.lineSequence()
        ?.joinToString(" ")
        ?.take(LOG_TEXT_LIMIT)
        ?: "null"
