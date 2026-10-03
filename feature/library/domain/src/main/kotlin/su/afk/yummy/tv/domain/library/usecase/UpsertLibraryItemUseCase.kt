package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/**
 * Adds or replaces one local library item. Для новой записи (без [LibraryItem.addedAt]) проставляет
 * время добавления и отметки обновления списка и избранного.
 */
class UpsertLibraryItemUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(item: LibraryItem) {
        if (item.addedAt > 0L) return repository.add(item)
        val now = clock.nowMillis()
        repository.add(
            item.copy(
                addedAt = now,
                listUpdatedAt = item.listUpdatedAt.takeIf { it > 0L } ?: now,
                favoriteUpdatedAt = item.favoriteUpdatedAt.takeIf { it > 0L }
                    ?: if (item.isFavorite) now else 0L,
            ),
        )
    }
}
