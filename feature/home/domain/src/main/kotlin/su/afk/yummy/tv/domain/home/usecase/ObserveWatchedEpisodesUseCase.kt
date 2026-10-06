package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Наблюдает за просмотренными сериями: id тайтла → нормализованные номера серий. */
class ObserveWatchedEpisodesUseCase @Inject constructor(
    private val repository: HomeFeedRepository,
) {
    operator fun invoke(): Flow<Map<Int, Set<Int>>> = repository.observeWatchedEpisodes()
}
