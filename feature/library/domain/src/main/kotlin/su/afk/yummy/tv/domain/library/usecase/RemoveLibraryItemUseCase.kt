package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

class RemoveLibraryItemUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(animeId: Int) = repository.remove(animeId)
}
