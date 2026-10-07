package su.afk.yummy.tv.feature.library.mobile.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.utils.semanticColorOrNull

internal fun LibraryTab.toLibraryMobilePage(tabs: List<LibraryTab>): Int =
    tabs.indexOf(this).coerceAtLeast(0)

internal fun Int.toLibraryMobileTab(tabs: List<LibraryTab>): LibraryTab =
    tabs.getOrElse(this) { LibraryTab.CONTINUE_WATCHING }

internal fun LibraryState.State.shouldShowRemoteLoader(tab: LibraryTab): Boolean {
    return !(!isSignedIn || !isRemoteLoading || remoteError != null) && when (tab) {
        LibraryTab.CONTINUE_WATCHING -> false
        LibraryTab.HISTORY -> false
        LibraryTab.FAVORITES -> mobileTabItemCount(tab) == 0
        LibraryTab.WATCHING,
        LibraryTab.PLANNED,
        LibraryTab.COMPLETED,
        LibraryTab.POSTPONED,
        LibraryTab.DROPPED -> mobileTabItemCount(tab) == 0
    }
}

internal fun LibraryState.State.mobileTabItemCount(tab: LibraryTab): Int? = when (tab) {
    LibraryTab.CONTINUE_WATCHING -> continueWatching.size
    LibraryTab.HISTORY -> null
    else -> tabItems[tab]?.size ?: 0
}

internal fun LibraryItem.posterUrl(): String? =
    poster.posterUrl()

private fun LibraryPoster?.posterUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

internal fun HomePoster?.posterUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

@Composable
internal fun LibraryTab.tabColor(): Color = when (this) {
    LibraryTab.CONTINUE_WATCHING -> MaterialTheme.colorScheme.primary
    LibraryTab.HISTORY -> MaterialTheme.colorScheme.tertiary
    else -> requireNotNull(semanticColorOrNull())
}
