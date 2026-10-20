package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/**
 * Записывает оценку пользователя в локальную библиотеку сразу после её выставления или снятия
 * (`null`), не дожидаясь синхронизации списков. Тайтлы вне библиотеки не затрагивает.
 */
class SetLibraryUserRatingUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(animeId: Int, rating: Int?) =
        repository.setUserRating(animeId, rating?.takeIf { it in 1..10 })
}
