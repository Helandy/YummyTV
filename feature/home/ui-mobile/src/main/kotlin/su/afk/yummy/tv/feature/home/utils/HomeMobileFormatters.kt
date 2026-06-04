package su.afk.yummy.tv.feature.home.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.feature.home.mobile.R
import java.util.Locale

internal fun HomeFeedSection.isTitleOnlySection(): Boolean =
    title == "Новинки" ||
        title == "New releases" ||
        title == "Коллекции" ||
        title == "Collections"

internal fun WatchProgressEntry.bestImageUrl(): String? =
    screenshotUrl.ifBlank { posterUrl }.ifBlank { null }

@Composable
internal fun WatchProgressEntry.progressSubtitle(): String =
    listOfNotNull(
        stringResource(R.string.home_mobile_episode, episode),
        dubbing.takeIf { it.isNotBlank() },
        playerName.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

internal fun WatchProgressEntry.watchProgress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

internal fun Double.asRatingBadge(): String = String.format(Locale.US, "%.1f", this)

internal fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
