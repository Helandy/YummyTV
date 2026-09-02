package su.afk.yummy.tv.feature.details.details.handler

import su.afk.yummy.tv.core.error.api.isNetworkError
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.storage.outbox.AnimeIdPayload
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
import su.afk.yummy.tv.core.storage.outbox.PendingMutationTypes
import su.afk.yummy.tv.core.storage.outbox.SetFavoritePayload
import su.afk.yummy.tv.core.storage.outbox.SetListPayload
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.library.usecase.RemoveLibraryItemUseCase
import su.afk.yummy.tv.domain.library.usecase.SetLibraryFavoriteUseCase
import su.afk.yummy.tv.domain.library.usecase.UpsertLibraryItemUseCase
import su.afk.yummy.tv.feature.details.mapper.toLibraryPoster
import su.afk.yummy.tv.feature.details.utils.toLibraryItem
import javax.inject.Inject

/**
 * Applies details-screen library and favorite mutations with local-first rollback support.
 *
 * A network failure does not roll back: the mutation is queued in [pendingMutationOutbox] and
 * retried once connectivity returns, so the optimistic local write stands. Only a non-network
 * failure (e.g. the server rejects the request) rolls the local write back.
 */
internal class DetailsLibraryHandler @Inject constructor(
    private val removeLibraryItem: RemoveLibraryItemUseCase,
    private val setLibraryFavorite: SetLibraryFavoriteUseCase,
    private val upsertLibraryItem: UpsertLibraryItemUseCase,
    private val getAnimeListState: GetAnimeListStateUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val pendingMutationOutbox: PendingMutationOutbox,
    private val pendingMutationSyncScheduler: PendingMutationSyncScheduler,
) {
    suspend fun refreshAuthorizedState(animeId: Int): Result<DetailsLibraryState?> =
        runCatching { getAnimeListState(animeId) }
            .map { state ->
                state ?: return@map null
                DetailsLibraryState(
                    isInLibrary = state.list != null,
                    libraryList = state.list,
                    isFavorite = state.isFavorite,
                )
            }

    suspend fun removeFromLibrary(
        animeId: Int,
        details: AnimeDetails,
        previousList: UserAnimeList?,
        wasInLibrary: Boolean,
        isFavorite: Boolean,
        isSignedIn: Boolean,
    ): DetailsLibraryMutationResult {
        removeLibraryItem(animeId)
        if (!isSignedIn || previousList == null) return DetailsLibraryMutationResult.Success

        val result = runCatching { removeAnimeList(animeId) }
        if (result.isSuccess) return DetailsLibraryMutationResult.Success

        if (result.queueOnNetworkFailure(
                PendingMutationTypes.REMOVE_LIST,
                AnimeIdPayload(animeId).encode()
            )
        ) {
            return DetailsLibraryMutationResult.Success
        }

        upsertLibraryItem(details.toLibraryItem(previousList, isFavorite))
        return DetailsLibraryMutationResult.RollbackLibrary(
            isInLibrary = wasInLibrary,
            libraryList = previousList,
        )
    }

    suspend fun addToLibrary(
        animeId: Int,
        details: AnimeDetails,
        list: UserAnimeList,
        wasInLibrary: Boolean,
        previousList: UserAnimeList?,
        isFavorite: Boolean,
        isSignedIn: Boolean,
    ): DetailsLibraryMutationResult {
        upsertLibraryItem(details.toLibraryItem(list, isFavorite))
        if (!isSignedIn) return DetailsLibraryMutationResult.Success

        val result = runCatching { setAnimeList(animeId, list) }
        if (result.isSuccess) return DetailsLibraryMutationResult.Success

        if (result.queueOnNetworkFailure(
                PendingMutationTypes.SET_LIST,
                SetListPayload(animeId, list.id).encode()
            )
        ) {
            return DetailsLibraryMutationResult.Success
        }

        if (wasInLibrary && previousList != null) {
            upsertLibraryItem(details.toLibraryItem(previousList, isFavorite))
        } else {
            removeLibraryItem(animeId)
        }
        return DetailsLibraryMutationResult.RollbackLibrary(
            isInLibrary = wasInLibrary,
            libraryList = previousList,
        )
    }

    suspend fun setFavorite(
        animeId: Int,
        details: AnimeDetails,
        favorite: Boolean,
        previousFavorite: Boolean,
        isSignedIn: Boolean,
    ): DetailsLibraryMutationResult {
        setLibraryFavorite(
            animeId = details.id,
            title = details.title,
            poster = details.poster?.toLibraryPoster(),
            year = details.year,
            favorite = favorite,
        )
        if (!isSignedIn) return DetailsLibraryMutationResult.Success

        val result = runCatching { setAnimeFavorite(animeId, favorite) }
        if (result.isSuccess) return DetailsLibraryMutationResult.Success

        if (result.queueOnNetworkFailure(
                PendingMutationTypes.SET_FAVORITE,
                SetFavoritePayload(animeId, favorite).encode()
            )
        ) {
            return DetailsLibraryMutationResult.Success
        }

        setLibraryFavorite(
            animeId = details.id,
            title = details.title,
            poster = details.poster?.toLibraryPoster(),
            year = details.year,
            favorite = previousFavorite,
        )
        return DetailsLibraryMutationResult.RollbackFavorite(previousFavorite)
    }

    /**
     * Ставит мутацию в offline-очередь, если провал вызван сетевой ошибкой, и сообщает вызывающей
     * стороне не откатывать оптимистичную запись. Для не сетевых ошибок ничего не делает — вызывающая
     * сторона откатывает как раньше.
     */
    private suspend fun Result<*>.queueOnNetworkFailure(
        type: String,
        payloadJson: String
    ): Boolean {
        val error = exceptionOrNull() ?: return false
        if (!error.isNetworkError()) return false
        pendingMutationOutbox.enqueue(type, payloadJson)
        pendingMutationSyncScheduler.scheduleFlush()
        return true
    }
}

/** Current user's remote list/favorite state for a details screen. */
internal data class DetailsLibraryState(
    val isInLibrary: Boolean,
    val libraryList: UserAnimeList?,
    val isFavorite: Boolean,
)

/** Outcome of a library mutation after local and remote updates are attempted. */
internal sealed interface DetailsLibraryMutationResult {
    data object Success : DetailsLibraryMutationResult
    data class RollbackLibrary(
        val isInLibrary: Boolean,
        val libraryList: UserAnimeList?,
    ) : DetailsLibraryMutationResult

    data class RollbackFavorite(val isFavorite: Boolean) : DetailsLibraryMutationResult
}
