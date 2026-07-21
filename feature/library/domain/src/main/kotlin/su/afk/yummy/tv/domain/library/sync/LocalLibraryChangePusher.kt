package su.afk.yummy.tv.domain.library.sync

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.updatedAtMillis
import javax.inject.Inject

internal class LocalLibraryChangePusher @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val setAnimeList: SetAnimeListUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
) {

    suspend fun push(
        remote: RemoteLibrarySnapshot,
        allowMissingRemoteUpload: Boolean,
        remoteFetchedAt: Long,
    ): LocalLibraryPushResult {
        val remoteLists = remote.lists
            .flatMap { (list, items) ->
                items.map { item -> item.animeId to RemoteListItem(list, item) }
            }
            .toMap()
        val remoteFavorites = remote.favorites.associateBy(UserAnimeListItem::animeId)
        var changedRemote = false
        var firstError: Throwable? = null

        libraryRepository.getAll().forEach { local ->
            val localList = local.userAnimeList()
            if (
                localList != null && shouldPushList(
                    local = local,
                    localList = localList,
                    remote = remoteLists[local.animeId],
                    allowMissingRemoteUpload = allowMissingRemoteUpload,
                    remoteFetchedAt = remoteFetchedAt,
                )
            ) {
                mutationResult { setAnimeList(local.animeId, localList) }
                    .onSuccess { changedRemote = true }
                    .onFailure { if (firstError == null) firstError = it }
            }

            if (
                shouldPushFavorite(
                    local = local,
                    remote = remoteFavorites[local.animeId],
                    allowMissingRemoteUpload = allowMissingRemoteUpload,
                    remoteFetchedAt = remoteFetchedAt,
                )
            ) {
                mutationResult { setAnimeFavorite(local.animeId, local.isFavorite) }
                    .onSuccess { changedRemote = true }
                    .onFailure { if (firstError == null) firstError = it }
            }
        }
        return LocalLibraryPushResult(changedRemote, firstError)
    }

    private suspend fun mutationResult(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private companion object {
        fun shouldPushList(
            local: LibraryItem,
            localList: UserAnimeList,
            remote: RemoteListItem?,
            allowMissingRemoteUpload: Boolean,
            remoteFetchedAt: Long,
        ): Boolean {
            remote ?: return allowMissingRemoteUpload
            return remote.list != localList &&
                    local.listUpdatedAt > remote.item.updatedAtMillis(remoteFetchedAt)
        }

        fun shouldPushFavorite(
            local: LibraryItem,
            remote: UserAnimeListItem?,
            allowMissingRemoteUpload: Boolean,
            remoteFetchedAt: Long,
        ): Boolean {
            if (local.isFavorite && remote == null) return allowMissingRemoteUpload
            if (remote == null) return false
            return !local.isFavorite &&
                    local.favoriteUpdatedAt > remote.updatedAtMillis(remoteFetchedAt)
        }

        fun LibraryItem.userAnimeList(): UserAnimeList? =
            UserAnimeList.entries.firstOrNull { it.id == listId }
    }
}
