package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/** Adds or replaces one local library item. */
class UpsertLibraryItemUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(item: LibraryItem) = repository.add(item)
}
