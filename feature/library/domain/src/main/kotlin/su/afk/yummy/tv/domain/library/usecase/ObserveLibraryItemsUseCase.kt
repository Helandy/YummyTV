package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

class ObserveLibraryItemsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeAll()
}
