package su.afk.yummy.tv.domain.library.usecase

import kotlinx.coroutines.flow.combine
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/** Наблюдает, находится ли выбранное аниме в библиотеке и избранном. */
class ObserveAnimeLibraryStateUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke(animeId: Int) = combine(
        repository.observeIsInLibrary(animeId),
        repository.observeIsFavorite(animeId),
    ) { isInLibrary, isFavorite ->
        AnimeLibraryState(
            isInLibrary = isInLibrary,
            isFavorite = isFavorite,
        )
    }
}

data class AnimeLibraryState(
    val isInLibrary: Boolean,
    val isFavorite: Boolean,
)
