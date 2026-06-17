package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/** Наблюдает за локальными элементами библиотеки пользователя. */
class ObserveLibraryItemsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeAll()
}
