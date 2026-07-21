package su.afk.yummy.tv.feature.home.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.feature.home.mobile.R

internal fun HomeFeedSectionType.showMobileCardMetadata(): Boolean = when (this) {
    HomeFeedSectionType.SCHEDULE,
    HomeFeedSectionType.NEW_RELEASES,
    HomeFeedSectionType.RECOMMENDATIONS,
    HomeFeedSectionType.COLLECTIONS -> false
}

@Composable
internal fun HomeContinueWatchingItem.episodeSubtitle(): String =
    if (episode.isBlank()) {
        stringResource(R.string.home_mobile_episode_unknown)
    } else {
        stringResource(R.string.home_mobile_episode, episode)
    }

internal fun HomeContinueWatchingItem.timingSubtitle(): String =
    if (durationMs > 0L) {
        "${positionMs.toMobileTimeString()} / ${durationMs.toMobileTimeString()}"
    } else {
        positionMs.toMobileTimeString()
    }

internal fun HomeContinueWatchingItem.watchProgress(): Float =
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
