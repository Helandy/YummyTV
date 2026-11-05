package su.afk.yummy.tv.feature.videodownload.handler

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeEpisodeFlagsUseCase
import javax.inject.Inject

/** Пометки серий (филлер/рекап) скачанных тайтлов: animeId → номер серии → флаги. */
typealias VideoDownloadEpisodeFlags = ImmutableMap<Int, ImmutableMap<String, EpisodeFlags>>

/**
 * Подписка на пометки серий для тайтлов из «Скачанного». Данные и настройка те же, что у списка серий и плеера;
 * сами пометки лежат в кэше документов (7 дней), поэтому сеть дёргается только при первом показе тайтла.
 */
class VideoDownloadEpisodeFlagsHandler @Inject constructor(
    private val observeEpisodeFlags: ObserveAnimeEpisodeFlagsUseCase,
    private val appearanceSettings: AppearanceSettingsStore,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(animeIds: Flow<Set<Int>>): Flow<VideoDownloadEpisodeFlags> =
        appearanceSettings.showEpisodeFlags
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) {
                    animeIds.distinctUntilChanged().flatMapLatest { ids -> observeTitles(ids.toList()) }
                } else {
                    flowOf(persistentMapOf())
                }
            }

    private fun observeTitles(ids: List<Int>): Flow<VideoDownloadEpisodeFlags> {
        if (ids.isEmpty()) return flowOf(persistentMapOf())
        return combine(
            ids.map { id ->
                observeEpisodeFlags(id)
                    .map { flags -> flags.mapKeys { (episode, _) -> episode.episodeGroupKey() }.toImmutableMap() }
                    .catch { emit(persistentMapOf()) }
                    .onStart { emit(persistentMapOf()) }
            },
        ) { perTitle ->
            ids.zip(perTitle).toMap().toImmutableMap()
        }
    }
}
