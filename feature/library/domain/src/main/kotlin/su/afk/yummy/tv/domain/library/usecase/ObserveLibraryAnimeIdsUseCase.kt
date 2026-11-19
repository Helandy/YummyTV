package su.afk.yummy.tv.domain.library.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.toLibraryAnimeIds
import javax.inject.Inject

/**
 * Наблюдает множество `animeId` тайтлов из локальной библиотеки пользователя
 * (в любом из списков или в избранном). Работает без сетевых запросов.
 */
class ObserveLibraryAnimeIdsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke(): Flow<Set<Int>> = repository.observeAll()
        .map { items -> items.toLibraryAnimeIds() }
        .distinctUntilChanged()
}
