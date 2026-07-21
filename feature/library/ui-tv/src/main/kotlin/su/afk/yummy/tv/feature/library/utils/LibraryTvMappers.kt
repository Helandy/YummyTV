package su.afk.yummy.tv.feature.library.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomePoster
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab

internal fun LibraryState.State.tvTabItemCount(tab: LibraryTab): Int = when (tab) {
    LibraryTab.CONTINUE_WATCHING -> continueWatching.size
    LibraryTab.HISTORY -> 0
    LibraryTab.FAVORITES -> items.count { it.isFavorite }

    LibraryTab.WATCHING,
    LibraryTab.PLANNED,
    LibraryTab.COMPLETED,
    LibraryTab.POSTPONED,
    LibraryTab.DROPPED -> {
        val localListId = tab.userAnimeListId()
        items.count { it.listId == localListId }
    }
}

internal fun LibraryTab.userAnimeListId(): Int? = when (this) {
    LibraryTab.CONTINUE_WATCHING -> null
    LibraryTab.HISTORY -> null
    LibraryTab.FAVORITES -> null
    LibraryTab.WATCHING -> UserAnimeList.WATCHING.id
    LibraryTab.PLANNED -> UserAnimeList.PLANNED.id
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED.id
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED.id
    LibraryTab.DROPPED -> UserAnimeList.DROPPED.id
}

internal fun LibraryTab.focusStateKey(source: String): String = "${name}_$source"

internal fun HomeContinueWatchingItem.continueWatchingFocusKey(): String =
    "$animeId:$videoId:$episode:$episodeUrl"

internal fun HomePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

internal fun LibraryItem.posterUrl(quality: PosterQuality): String? = poster.posterUrl(quality)

private fun LibraryPoster?.posterUrl(quality: PosterQuality): String? = when (quality) {
    PosterQuality.LOW -> this?.medium ?: this?.big ?: this?.fullsize ?: this?.small
    PosterQuality.STANDARD -> this?.big ?: this?.medium ?: this?.fullsize ?: this?.small
    PosterQuality.MEGA -> this?.mega ?: this?.big ?: this?.medium ?: this?.fullsize ?: this?.small
    PosterQuality.HIGH -> this?.fullsize ?: this?.mega ?: this?.big ?: this?.medium ?: this?.small
}

@Composable
internal fun LibraryTab.tvTabColor(): Color = when (this) {
    LibraryTab.CONTINUE_WATCHING -> MaterialTheme.colorScheme.primary
    LibraryTab.HISTORY -> MaterialTheme.colorScheme.tertiary
    LibraryTab.WATCHING -> Color(0xFFFF6B6B)
    LibraryTab.PLANNED -> Color(0xFFA678E8)
    LibraryTab.COMPLETED -> Color(0xFF69D38B)
    LibraryTab.POSTPONED -> Color(0xFFFFC857)
    LibraryTab.DROPPED -> Color(0xFF9CA3AF)
    LibraryTab.FAVORITES -> Color(0xFFD86BFF)
}
