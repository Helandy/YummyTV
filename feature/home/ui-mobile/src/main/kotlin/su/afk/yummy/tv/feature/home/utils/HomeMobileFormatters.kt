package su.afk.yummy.tv.feature.home.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.feature.home.mobile.R

internal fun HomeFeedSection.isTitleOnlySection(): Boolean =
    title == "Новинки" ||
        title == "New releases" ||
            title == "Рекомендации" ||
            title == "Recommendations" ||
        title == "Коллекции" ||
        title == "Collections"

internal suspend fun WatchProgressEntry.resolveMobileContinueWatchingImage(): String? {
    val kodikScreenshot = screenshotUrl.takeIf { it.isKodikSourceUrl() }
    return kodikScreenshot?.let { KodikThumbnailExtractor.extract(it) }
        ?: episodeUrl.takeIf { it.isNotBlank() }?.let { KodikThumbnailExtractor.extract(it) }
        ?: posterUrl.ifBlank { null }
}

@Composable
internal fun WatchProgressEntry.episodeSubtitle(): String =
    stringResource(R.string.home_mobile_episode, episode)

internal fun WatchProgressEntry.timingSubtitle(): String =
    "${positionMs.toMobileTimeString()} / ${durationMs.toMobileTimeString()}"

internal fun WatchProgressEntry.watchProgress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

internal fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

private fun Long.toMobileTimeString(): String {
    val totalSec = this.coerceAtLeast(0L) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)
