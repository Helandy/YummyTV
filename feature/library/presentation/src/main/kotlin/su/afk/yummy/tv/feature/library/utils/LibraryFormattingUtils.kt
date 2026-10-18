package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.utils.formatting.formatRelativeDateTime
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.core.utils.formatting.secondsToClockTime
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.core.utils.formatting.formatLocalizedDateMillis

fun LibraryItem.dateText(tab: LibraryTab): String? =
    when (tab) {
        LibraryTab.FAVORITES -> favoriteUpdatedAt
        LibraryTab.CONTINUE_WATCHING -> 0L
        LibraryTab.HISTORY -> 0L
        else -> listUpdatedAt
    }.formatLibraryDate()

private fun Long.formatLibraryDate(): String? =
    takeIf { it > 0L }?.formatLocalizedDateMillis()

fun HomeContinueWatchingItem.timingLabel(): String? =
    if (durationMs > 0L) {
        "${positionMs.millisToClockTime()} / ${durationMs.millisToClockTime()}"
    } else {
        positionMs.millisToClockTime()
    }

fun WatchHistoryEntry.watchedAtLabel(nowMillis: Long): String? =
    watchedAtSeconds.takeIf { it > 0 }?.formatRelativeDateTime(nowMillis)

fun WatchHistoryEntry.timingLabel(): String? =
    if (durationSeconds > 0) {
        "${positionSeconds.secondsToClockTime()} / ${durationSeconds.secondsToClockTime()}"
    } else {
        null
    }
