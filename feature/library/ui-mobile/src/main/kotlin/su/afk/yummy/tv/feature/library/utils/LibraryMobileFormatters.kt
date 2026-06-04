package su.afk.yummy.tv.feature.library.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.mobile.R

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
