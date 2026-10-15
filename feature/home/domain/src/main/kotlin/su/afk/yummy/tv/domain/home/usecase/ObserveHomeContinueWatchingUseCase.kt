package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.mergeWithRemote
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.domain.home.repository.RemoteContinueWatchingRepository
import javax.inject.Inject

/**
 * Наблюдает за «Продолжить просмотр» для главной: локальный прогресс, дополненный серверной
 * историей авторизованного пользователя, по карточке на тайтл и в порядке свежести, не больше 8.
 * Досмотренная на сервере серия превращается в карточку следующей серии, если включена настройка
 * «Следующая серия».
 */
class ObserveHomeContinueWatchingUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
    private val remoteRepository: RemoteContinueWatchingRepository,
) {
    operator fun invoke(): Flow<List<HomeContinueWatchingItem>> =
        combine(
            homeFeedRepository.observeContinueWatching(),
            remoteRepository.observeLastWatches(),
            homeFeedRepository.observeWatchHistory(),
            homeFeedRepository.observeContinueWatchingSuppressions(),
            homeFeedRepository.observeSuggestNextEpisodeOnWatched(),
        ) { local, remote, history, suppressedAt, suggestNextEpisode ->
            local.mergeWithRemote(
                remote = remote,
                localHistory = history,
                suppressedAt = suppressedAt,
                suggestNextEpisode = suggestNextEpisode,
            ).take(HOME_CONTINUE_WATCHING_LIMIT)
        }.distinctUntilChanged()
}

private const val HOME_CONTINUE_WATCHING_LIMIT = 8
