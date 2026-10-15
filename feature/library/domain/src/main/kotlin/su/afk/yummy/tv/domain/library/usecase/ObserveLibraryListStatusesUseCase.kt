package su.afk.yummy.tv.domain.library.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.toCardListStatuses
import javax.inject.Inject

/**
 * Наблюдает карту `animeId → список пользователя` по локальной библиотеке.
 *
 * Тайтлы, которые только в избранном (вне списков), в карту не попадают. Работает без сети:
 * используется, чтобы подписывать карточки в топе, поиске и подборках.
 */
class ObserveLibraryListStatusesUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke(): Flow<Map<Int, CardListStatus>> = repository.observeAll()
        .map { items -> items.toCardListStatuses() }
        .distinctUntilChanged()
}
