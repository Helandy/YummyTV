package su.afk.yummy.tv.feature.details.details.handler

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStateUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.feature.details.utils.toLibraryItem
import su.afk.yummy.tv.feature.details.utils.toLibraryPoster
import javax.inject.Inject

/** Applies details-screen library and favorite mutations with local-first rollback support. */
internal class DetailsLibraryHandler @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val getAnimeListState: GetAnimeListStateUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
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
        libraryRepository.remove(animeId)
        if (!isSignedIn || previousList == null) return DetailsLibraryMutationResult.Success

        val result = runCatching { removeAnimeList(animeId) }
        if (result.isSuccess) return DetailsLibraryMutationResult.Success

        libraryRepository.add(details.toLibraryItem(previousList, isFavorite))
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
        libraryRepository.add(details.toLibraryItem(list, isFavorite))
        if (!isSignedIn) return DetailsLibraryMutationResult.Success

        val result = runCatching { setAnimeList(animeId, list) }
        if (result.isSuccess) return DetailsLibraryMutationResult.Success

        if (wasInLibrary && previousList != null) {
            libraryRepository.add(details.toLibraryItem(previousList, isFavorite))
        } else {
            libraryRepository.remove(animeId)
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
        libraryRepository.setFavorite(
            animeId = details.id,
            title = details.title,
            poster = details.poster?.toLibraryPoster(),
            favorite = favorite,
        )
        if (!isSignedIn) return DetailsLibraryMutationResult.Success

        val result = runCatching { setAnimeFavorite(animeId, favorite) }
        if (result.isSuccess) return DetailsLibraryMutationResult.Success

        libraryRepository.setFavorite(
            animeId = details.id,
            title = details.title,
            poster = details.poster?.toLibraryPoster(),
            favorite = previousFavorite,
        )
        return DetailsLibraryMutationResult.RollbackFavorite(previousFavorite)
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
