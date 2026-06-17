package su.afk.yummy.tv.feature.library.model

import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.library.LibraryRemoveTarget
import su.afk.yummy.tv.feature.library.LibraryState

internal sealed interface PendingLibraryMobileRemoval {
    val title: String
    val listTitle: String

    fun event(): LibraryState.Event

    data class WatchProgress(
        val entry: HomeContinueWatchingItem,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override val title = entry.animeTitle.ifBlank { entry.episode }
        override fun event(): LibraryState.Event =
            LibraryState.Event.RemoveWatchProgress(entry.animeId)
    }

    data class Favorite(
        val animeId: Int,
        override val title: String,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override fun event(): LibraryState.Event =
            LibraryState.Event.RemoveEntry(animeId, LibraryRemoveTarget.FAVORITE)
    }

    data class ListEntry(
        val animeId: Int,
        override val title: String,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override fun event(): LibraryState.Event =
            LibraryState.Event.RemoveEntry(animeId, LibraryRemoveTarget.LIST)
    }
}
