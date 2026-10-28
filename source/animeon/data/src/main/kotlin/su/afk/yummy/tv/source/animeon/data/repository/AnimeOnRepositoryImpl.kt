package su.afk.yummy.tv.source.animeon.data.repository

import su.afk.yummy.tv.source.animeon.data.network.AnimeOnApi
import su.afk.yummy.tv.source.animeon.data.utils.animeOnEpisodeId
import su.afk.yummy.tv.source.animeon.data.utils.animeOnEpisodesUrl
import su.afk.yummy.tv.source.animeon.data.utils.animeOnPlayerTrack
import su.afk.yummy.tv.source.animeon.data.utils.animeOnPlayerUrl
import su.afk.yummy.tv.source.animeon.data.utils.animeOnVideoUrl
import su.afk.yummy.tv.source.animeon.data.utils.isFenixPlayerUrl
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.animeon.domain.repository.AnimeOnRepository
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreams
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import su.afk.yummy.tv.source.ashdi.domain.repository.AshdiRepository
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import su.afk.yummy.tv.source.fenix.domain.repository.FenixRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AnimeOnRepositoryImpl @Inject constructor(
    private val api: AnimeOnApi,
    private val ashdi: AshdiRepository,
    private val fenix: FenixRepository,
) : AnimeOnRepository {

    /**
     * Цепочка: серия плеера на сайте → ссылка на страницу плеера → общий разбор Ashdi (`:source:ashdi`: `file:` страницы,
     * HLS-мастер, гео-блок) или Fenix (`:source:fenix`: дорожка озвучки из ответа сайта, мастер с токеном). Id серий у плееров
     * свои, поэтому цепочка читается в момент воспроизведения. Поток Fenix приходит как [AshdiStreams] без карты качеств: плеер
     * играет мастер адаптивно, а гео-блока у Fenix нет.
     */
    override suspend fun getStreams(target: AnimeOnTarget): AshdiStreamsResult {
        val episodes = api.getText(animeOnEpisodesUrl(target.titleKey, target.playerId, target.translationId, target.episode))
            ?: return AshdiStreamsResult.NotFound
        val episodeId = animeOnEpisodeId(episodes, target.episode) ?: return AshdiStreamsResult.NotFound
        val video = api.getText(animeOnVideoUrl(episodeId)) ?: return AshdiStreamsResult.NotFound
        val playerUrl = animeOnPlayerUrl(video) ?: return AshdiStreamsResult.NotFound
        if (!isFenixPlayerUrl(playerUrl)) return ashdi.getStreams(playerUrl)
        return when (val result = fenix.getStreams(playerUrl, animeOnPlayerTrack(video))) {
            is FenixStreamsResult.Success -> AshdiStreamsResult.Success(AshdiStreams(emptyMap(), result.streams.masterUrl))
            FenixStreamsResult.NotFound -> AshdiStreamsResult.NotFound
            is FenixStreamsResult.Failed -> AshdiStreamsResult.Failed(result.reason)
        }
    }
}
