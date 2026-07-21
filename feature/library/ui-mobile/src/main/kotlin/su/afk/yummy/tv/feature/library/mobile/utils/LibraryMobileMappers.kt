package su.afk.yummy.tv.feature.library.mobile.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab

internal val libraryMobileTabs: List<LibraryTab>
    get() = LibraryTab.visibleEntries

internal fun LibraryTab.userAnimeList(): UserAnimeList? = when (this) {
    LibraryTab.WATCHING -> UserAnimeList.WATCHING
    LibraryTab.PLANNED -> UserAnimeList.PLANNED
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED
    LibraryTab.DROPPED -> UserAnimeList.DROPPED
    LibraryTab.CONTINUE_WATCHING,
    LibraryTab.HISTORY,
    LibraryTab.FAVORITES -> null
}

internal fun LibraryTab.toLibraryMobilePage(): Int =
    libraryMobileTabs.indexOf(this).coerceAtLeast(0)

internal fun Int.toLibraryMobileTab(): LibraryTab =
    libraryMobileTabs.getOrElse(this) { LibraryTab.CONTINUE_WATCHING }

internal fun LibraryState.State.shouldShowRemoteLoader(tab: LibraryTab): Boolean {
    if (!isSignedIn || !isRemoteLoading || remoteError != null) return false
    return when (tab) {
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

internal fun LibraryState.State.mobileTabItemCount(tab: LibraryTab): Int = when (tab) {
    LibraryTab.CONTINUE_WATCHING -> continueWatching.size
    LibraryTab.HISTORY -> 0
    LibraryTab.FAVORITES -> items.count { it.isFavorite }
    LibraryTab.WATCHING,
    LibraryTab.PLANNED,
    LibraryTab.COMPLETED,
    LibraryTab.POSTPONED,
    LibraryTab.DROPPED -> {
        val localList = tab.userAnimeList()
        items.count { it.listId == localList?.id }
    }
}

internal fun LibraryItem.posterUrl(): String? =
    poster.posterUrl()

private fun LibraryPoster?.posterUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

internal fun HomePoster?.posterUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

@Composable
internal fun LibraryTab.mobileTabColor(): Color = when (this) {
    LibraryTab.CONTINUE_WATCHING -> MaterialTheme.colorScheme.primary
    LibraryTab.HISTORY -> MaterialTheme.colorScheme.tertiary
    LibraryTab.WATCHING -> Color(0xFFFF6B6B)
    LibraryTab.PLANNED -> Color(0xFFA678E8)
    LibraryTab.COMPLETED -> Color(0xFF69D38B)
    LibraryTab.POSTPONED -> Color(0xFFFFC857)
    LibraryTab.DROPPED -> Color(0xFF9CA3AF)
    LibraryTab.FAVORITES -> Color(0xFFD86BFF)
}
