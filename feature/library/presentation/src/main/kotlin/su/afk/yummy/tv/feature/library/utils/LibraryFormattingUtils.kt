package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.core.utils.formatting.formatRelativeDateTime
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.core.utils.formatting.secondsToClockTime
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.library.model.LibraryTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun LibraryItem.dateText(tab: LibraryTab): String? =
    when (tab) {
        LibraryTab.FAVORITES -> favoriteUpdatedAt
        LibraryTab.CONTINUE_WATCHING -> 0L
        LibraryTab.HISTORY -> 0L
        else -> listUpdatedAt
    }.formatLibraryDate()

fun LibraryItem.validUserRating(): Double? =
    userRating?.takeIf { it in 1..10 }?.toDouble()

private val libraryDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

private fun Long.formatLibraryDate(): String? =
    takeIf { it > 0L }?.let { libraryDateFormatter.format(Date(it)) }

fun HomeContinueWatchingItem.timingLabel(): String? =
    if (durationMs > 0L) {
        "${positionMs.millisToClockTime()} / ${durationMs.millisToClockTime()}"
    } else {
        positionMs.millisToClockTime()
    }

fun WatchHistoryEntry.watchedAtLabel(): String? =
    watchedAtSeconds.takeIf { it > 0 }?.formatRelativeDateTime()

fun WatchHistoryEntry.timingLabel(): String? =
    if (durationSeconds > 0) {
        "${positionSeconds.secondsToClockTime()} / ${durationSeconds.secondsToClockTime()}"
    } else {
        null
    }
