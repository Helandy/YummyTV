package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.domain.home.utils.matches
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryItemsUseCase
import javax.inject.Inject

/**
 * Наблюдает за id тайтлов, чьи новые серии интересны пользователю: берутся списки из [sources].
 * Работает по локальной библиотеке, поэтому не делает сетевых запросов на тайтл.
 */
class ObserveLibraryNewEpisodeAnimeIdsUseCase @Inject constructor(
    private val observeLibraryItems: ObserveLibraryItemsUseCase,
) {
    operator fun invoke(sources: Set<NewEpisodesSource>): Flow<Set<Int>> =
        observeLibraryItems().map { items ->
            if (sources.isEmpty()) {
                emptySet()
            } else {
                items
                    .filter { item -> sources.any { item.matches(it) } }
                    .mapTo(HashSet()) { it.animeId }
            }
        }
}
