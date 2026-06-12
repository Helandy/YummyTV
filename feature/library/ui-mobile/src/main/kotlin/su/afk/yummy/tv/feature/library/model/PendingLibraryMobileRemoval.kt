package su.afk.yummy.tv.feature.library.model

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.feature.library.LibraryState

internal sealed interface PendingLibraryMobileRemoval {
    val title: String
    val listTitle: String

    fun event(): LibraryState.Event

    data class WatchProgress(
        val entry: WatchProgressEntry,
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
        val isRemote: Boolean,
    ) : PendingLibraryMobileRemoval {
        override fun event(): LibraryState.Event =
            if (isRemote) {
                LibraryState.Event.RemoveRemoteEntry(animeId = animeId, favorite = true)
            } else {
                LibraryState.Event.RemoveFavoriteEntry(animeId)
            }
    }

    data class RemoteList(
        val item: UserAnimeListItem,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override val title = item.title
        override fun event(): LibraryState.Event =
            LibraryState.Event.RemoveRemoteEntry(item.animeId)
    }

    data class LocalList(
        val animeId: Int,
        override val title: String,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override fun event(): LibraryState.Event = LibraryState.Event.RemoveLibraryEntry(animeId)
    }
}
