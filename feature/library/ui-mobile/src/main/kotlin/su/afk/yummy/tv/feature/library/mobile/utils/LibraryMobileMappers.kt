package su.afk.yummy.tv.feature.library.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.components.color
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.mapper.toLibraryTabKind
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.utils.isLocalLoading

internal fun LibraryTab.toLibraryMobilePage(tabs: List<LibraryTab>): Int =
    tabs.indexOf(this).coerceAtLeast(0)

internal fun Int.toLibraryMobileTab(tabs: List<LibraryTab>): LibraryTab =
    tabs.getOrElse(this) { LibraryTab.CONTINUE_WATCHING }

internal fun LibraryState.State.shouldShowRemoteLoader(tab: LibraryTab): Boolean {
    if (isLocalLoading(tab)) return true
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

@Composable
internal fun LibraryTab.tabColor(): Color = toLibraryTabKind().color()

/** Иконка вкладки — общая с настройками порядка вкладок. */
internal fun LibraryTab.tabIcon(): ImageVector = toLibraryTabKind().icon()
