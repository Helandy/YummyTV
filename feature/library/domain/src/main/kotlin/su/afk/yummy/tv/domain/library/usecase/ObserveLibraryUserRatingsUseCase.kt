package su.afk.yummy.tv.domain.library.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.toUserRatings
import javax.inject.Inject

/**
 * Наблюдает карту `animeId → оценка пользователя` по локальной библиотеке.
 *
 * Тайтлы без оценки в карту не попадают. Работает без сети: используется, чтобы в чужих списках
 * показывать рядом с оценкой владельца и свою.
 */
class ObserveLibraryUserRatingsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke(): Flow<Map<Int, Int>> = repository.observeAll()
        .map { items -> items.toUserRatings() }
        .distinctUntilChanged()
}
