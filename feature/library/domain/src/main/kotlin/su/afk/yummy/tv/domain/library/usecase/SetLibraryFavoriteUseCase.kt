package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/** Обновляет локальный признак избранного для тайтла. */
class SetLibraryFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(
        animeId: Int,
        title: String,
        poster: AnimePoster?,
        year: Int?,
        favorite: Boolean,
    ) = repository.setFavorite(animeId, title, poster, year, favorite)
}
