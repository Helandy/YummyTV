package su.afk.yummy.tv.feature.library.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.mobile.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun LibraryTab.mobileTitle(): String = when (this) {
    LibraryTab.CONTINUE_WATCHING -> stringResource(R.string.library_mobile_tab_continue_watching)
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
