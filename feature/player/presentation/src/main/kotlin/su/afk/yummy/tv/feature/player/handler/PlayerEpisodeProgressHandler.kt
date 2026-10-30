package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeWatchProgressUseCase
import su.afk.yummy.tv.feature.player.host.PlayerStateHost
import javax.inject.Inject

/** Подписка на локальный прогресс серий тайтла: из него строятся подписи в списке серий плеера. */
internal class PlayerEpisodeProgressHandler @Inject constructor(
    private val observeWatchProgress: ObserveAnimeWatchProgressUseCase,
) {
    private var job: Job? = null

    fun observe(host: PlayerStateHost, animeId: Int) {
        job?.cancel()
        if (animeId <= 0) return
        job = observeWatchProgress(animeId)
            .onEach { progress -> host.update { copy(watchProgress = progress) } }
            .launchIn(host.scope)
    }
}
