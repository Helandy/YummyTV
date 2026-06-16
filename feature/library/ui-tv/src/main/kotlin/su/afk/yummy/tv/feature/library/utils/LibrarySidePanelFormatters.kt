package su.afk.yummy.tv.feature.library.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.R

internal fun libraryTabsDisplayOrder(): List<LibraryTab> =
    LibraryTab.entries

@Composable
internal fun LibraryTab.label(): String = stringResource(
    when (this) {
        LibraryTab.CONTINUE_WATCHING -> R.string.library_tab_continue_watching
        LibraryTab.FAVORITES -> R.string.library_tab_favorites
        LibraryTab.WATCHING -> R.string.library_tab_watching
        LibraryTab.PLANNED -> R.string.library_tab_planned
        LibraryTab.COMPLETED -> R.string.library_tab_completed
        LibraryTab.POSTPONED -> R.string.library_tab_postponed
        LibraryTab.DROPPED -> R.string.library_tab_dropped
    },
)

@Composable
internal fun LibraryTab.shortLabel(): String = stringResource(
    when (this) {
        LibraryTab.CONTINUE_WATCHING -> R.string.library_tab_continue_watching_short
        LibraryTab.FAVORITES -> R.string.library_tab_favorites_short
        LibraryTab.WATCHING -> R.string.library_tab_watching_short
        LibraryTab.PLANNED -> R.string.library_tab_planned_short
        LibraryTab.COMPLETED -> R.string.library_tab_completed_short
        LibraryTab.POSTPONED -> R.string.library_tab_postponed_short
        LibraryTab.DROPPED -> R.string.library_tab_dropped_short
    },
)
