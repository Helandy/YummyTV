package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.mergeWithRemote
import su.afk.yummy.tv.domain.home.model.reconcileWithEpisodeProgress
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.domain.home.repository.RemoteContinueWatchingRepository
import javax.inject.Inject

/**
 * Наблюдает за «Продолжить просмотр» для главной: локальный прогресс, дополненный серверной
 * историей авторизованного пользователя, по карточке на тайтл и в порядке свежести, не больше 8.
 * Досмотренная на сервере серия превращается в карточку следующей серии, если включена настройка
 * «Следующая серия».
 * Затем карточки сверяются с серверным прогрессом из кэша `/videos` по тому же правилу, что и список
 * серий тайтла: серия, которую детали считают просмотренной, не висит в «Продолжить просмотр».
 */
class ObserveHomeContinueWatchingUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
    private val remoteRepository: RemoteContinueWatchingRepository,
) {
    operator fun invoke(): Flow<List<HomeContinueWatchingItem>> =
        combine(
            sources(),
            homeFeedRepository.observeServerWatchProgress(),
            homeFeedRepository.observeAvailableEpisodes(),
        ) { sources, serverProgress, availableEpisodes ->
            sources.local.mergeWithRemote(
                remote = sources.remote,
                localHistory = sources.history,
                suppressedAt = sources.suppressedAt,
                suggestNextEpisode = sources.suggestNextEpisode,
                availableEpisodes = availableEpisodes,
            ).reconcileWithEpisodeProgress(
                serverProgress = serverProgress,
                localHistory = sources.history,
                suggestNextEpisode = sources.suggestNextEpisode,
                availableEpisodes = availableEpisodes,
            ).take(HOME_CONTINUE_WATCHING_LIMIT)
        }.distinctUntilChanged()

    private fun sources(): Flow<Sources> =
        combine(
            homeFeedRepository.observeContinueWatching(),
            remoteRepository.observeLastWatches(),
            homeFeedRepository.observeWatchHistory(),
            homeFeedRepository.observeContinueWatchingSuppressions(),
            homeFeedRepository.observeSuggestNextEpisodeOnWatched(),
        ) { local, remote, history, suppressedAt, suggestNextEpisode ->
            Sources(local, remote, history, suppressedAt, suggestNextEpisode)
        }

    private class Sources(
        val local: List<HomeContinueWatchingItem>,
        val remote: List<HomeContinueWatchingItem>,
        val history: List<HomeContinueWatchingItem>,
        val suppressedAt: Map<Int, Long>,
        val suggestNextEpisode: Boolean,
    )
}

private const val HOME_CONTINUE_WATCHING_LIMIT = 8
