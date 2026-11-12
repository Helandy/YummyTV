package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.domain.home.utils.unionWatched
import su.afk.yummy.tv.domain.home.utils.watchedEpisodeNumbers
import javax.inject.Inject

/**
 * Наблюдает за просмотренными сериями: id тайтла → нормализованные номера серий. Объединяет
 * локальные отметки и серверные `watched` из кэша `/videos` — серия, досмотренная на другом
 * устройстве, считается просмотренной, как только деталка тайтла загрузила список видео.
 */
class ObserveWatchedEpisodesUseCase @Inject constructor(
    private val repository: HomeFeedRepository,
) {
    operator fun invoke(): Flow<Map<Int, Set<Int>>> =
        combine(
            repository.observeWatchedEpisodes(),
            repository.observeServerWatchProgress(),
        ) { local, server ->
            local.unionWatched(server.watchedEpisodeNumbers())
        }.distinctUntilChanged()
}
