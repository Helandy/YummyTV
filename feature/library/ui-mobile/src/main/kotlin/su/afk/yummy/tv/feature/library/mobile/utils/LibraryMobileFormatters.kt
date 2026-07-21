package su.afk.yummy.tv.feature.library.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.mobile.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun LibraryTab.mobileTitle(): String = when (this) {
    LibraryTab.CONTINUE_WATCHING -> stringResource(R.string.library_mobile_tab_continue_watching)
    LibraryTab.HISTORY -> stringResource(R.string.library_mobile_tab_history)
    LibraryTab.FAVORITES -> stringResource(R.string.library_mobile_tab_favorites)
    LibraryTab.WATCHING -> stringResource(R.string.library_mobile_tab_watching)
    LibraryTab.PLANNED -> stringResource(R.string.library_mobile_tab_planned)
    LibraryTab.COMPLETED -> stringResource(R.string.library_mobile_tab_completed)
    LibraryTab.POSTPONED -> stringResource(R.string.library_mobile_tab_postponed)
    LibraryTab.DROPPED -> stringResource(R.string.library_mobile_tab_dropped)
}

@Composable
internal fun LibraryItem.mobileDateSubtitle(tab: LibraryTab): String? {
    val date = mobileDateText(tab)
    return date?.let { stringResource(R.string.library_mobile_added_date, it) }
}

internal fun LibraryItem.mobileDateText(tab: LibraryTab): String? {
    val date = when (tab) {
        LibraryTab.FAVORITES -> favoriteUpdatedAt
        LibraryTab.CONTINUE_WATCHING -> 0L
        LibraryTab.HISTORY -> 0L
        else -> listUpdatedAt
    }.formatLibraryDate()
    return date
}

internal fun LibraryItem.mobileUserRating(): Double? =
    userRating?.takeIf { it in 1..10 }?.toDouble()

private fun Long.formatLibraryDate(): String? =
    takeIf { it > 0L }?.let {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
    }

internal fun HomeContinueWatchingItem.watchProgress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

internal fun HomeContinueWatchingItem.timingLabel(): String? =
    if (durationMs > 0L) {
        "${positionMs.toTimeString()} / ${durationMs.toTimeString()}"
    } else {
        positionMs.toTimeString()
    }

private fun Long.toTimeString(): String {
    val totalSec = coerceAtLeast(0L) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
