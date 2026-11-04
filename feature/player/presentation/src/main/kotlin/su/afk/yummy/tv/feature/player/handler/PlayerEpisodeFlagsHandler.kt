package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeEpisodeFlagsUseCase
import su.afk.yummy.tv.feature.player.host.PlayerStateHost
import javax.inject.Inject

/** Подписка на пометки серий (филлер/рекап): те же данные и та же настройка, что у списка серий тайтла. */
internal class PlayerEpisodeFlagsHandler @Inject constructor(
    private val observeEpisodeFlags: ObserveAnimeEpisodeFlagsUseCase,
    private val appearanceSettings: AppearanceSettingsStore,
) {
    private var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(host: PlayerStateHost, animeId: Int) {
        job?.cancel()
        if (animeId <= 0) return
        job = appearanceSettings.showEpisodeFlags
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) {
                    observeEpisodeFlags(animeId)
                        .map { flags -> flags.mapKeys { (episode, _) -> episode.episodeGroupKey() } }
                        .catch { emit(emptyMap()) }
                } else {
                    flowOf(emptyMap())
                }
            }
            .onEach { flags -> host.update { copy(episodeFlags = flags) } }
            .launchIn(host.scope)
    }
}
