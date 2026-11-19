package su.afk.yummy.tv.domain.library.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.ACTIVE_LIBRARY_LIST_STATUSES
import su.afk.yummy.tv.domain.library.utils.toLibraryAnimeIds
import javax.inject.Inject

/**
 * Наблюдает множество `animeId` тайтлов из локальной библиотеки пользователя.
 *
 * По умолчанию включает только активные списки ([ACTIVE_LIBRARY_LIST_STATUSES]: «Смотрю»,
 * «Запланировано») и избранное, исключая «Брошено», «Отложено» и «Просмотрено».
 * Работает без сетевых запросов.
 */
class ObserveLibraryAnimeIdsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke(
        statuses: Set<CardListStatus> = ACTIVE_LIBRARY_LIST_STATUSES,
        includeFavorites: Boolean = true,
    ): Flow<Set<Int>> = repository.observeAll()
        .map { items ->
            items.toLibraryAnimeIds(
                statuses = statuses,
                includeFavorites = includeFavorites,
            )
        }
        .distinctUntilChanged()
}
