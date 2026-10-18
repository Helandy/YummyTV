package su.afk.yummy.tv.feature.library.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.presentation.R

@Composable
internal fun LibraryTab.mobileTitle(): String = when (this) {
    LibraryTab.CONTINUE_WATCHING -> stringResource(R.string.library_tab_continue_watching)
    LibraryTab.HISTORY -> stringResource(R.string.library_tab_history)
    LibraryTab.FAVORITES -> stringResource(R.string.library_tab_favorites)
    LibraryTab.WATCHING -> stringResource(R.string.library_tab_watching)
    LibraryTab.PLANNED -> stringResource(R.string.library_tab_planned)
    LibraryTab.COMPLETED -> stringResource(R.string.library_tab_completed)
    LibraryTab.POSTPONED -> stringResource(R.string.library_tab_postponed)
    LibraryTab.DROPPED -> stringResource(R.string.library_tab_dropped)
}

@Composable
internal fun LibrarySort.mobileLabel(): String = when (this) {
    LibrarySort.ADDED_DATE -> stringResource(R.string.library_mobile_sort_added_date)
    LibrarySort.YEAR -> stringResource(R.string.library_mobile_sort_year)
    LibrarySort.RATING -> stringResource(R.string.library_mobile_sort_rating)
    LibrarySort.USER_RATING -> stringResource(R.string.library_mobile_sort_user_rating)
    LibrarySort.TITLE -> stringResource(R.string.library_mobile_sort_title)
}

internal fun HomeContinueWatchingItem.watchProgress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
