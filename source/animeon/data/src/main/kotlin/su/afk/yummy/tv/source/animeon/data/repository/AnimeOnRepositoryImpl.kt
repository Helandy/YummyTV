package su.afk.yummy.tv.source.animeon.data.repository

import su.afk.yummy.tv.source.animeon.data.network.AnimeOnApi
import su.afk.yummy.tv.source.animeon.data.utils.animeOnEpisodeId
import su.afk.yummy.tv.source.animeon.data.utils.animeOnEpisodesUrl
import su.afk.yummy.tv.source.animeon.data.utils.animeOnPlayerTrack
import su.afk.yummy.tv.source.animeon.data.utils.animeOnPlayerUrl
import su.afk.yummy.tv.source.animeon.data.utils.animeOnVideoUrl
import su.afk.yummy.tv.source.animeon.data.utils.isFenixPlayerUrl
import su.afk.yummy.tv.source.animeon.data.utils.isMoonPlayerUrl
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreams
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreamsResult
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.animeon.domain.repository.AnimeOnRepository
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import su.afk.yummy.tv.source.ashdi.domain.repository.AshdiRepository
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import su.afk.yummy.tv.source.fenix.domain.repository.FenixRepository
import su.afk.yummy.tv.source.moon.domain.model.MoonStreamsResult
import su.afk.yummy.tv.source.moon.domain.repository.MoonRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AnimeOnRepositoryImpl @Inject constructor(
    private val api: AnimeOnApi,
    private val ashdi: AshdiRepository,
    private val fenix: FenixRepository,
    private val moon: MoonRepository,
) : AnimeOnRepository {

    /**
     * Цепочка: серия плеера на сайте → ссылка на страницу плеера → общий разбор Ashdi (`:source:ashdi`: `file:` страницы,
     * HLS-мастер, гео-блок), Fenix (`:source:fenix`: дорожка озвучки из ответа сайта, мастер с токеном) или Moon (`:source:moon`:
     * скрипт страницы, заголовки потока). Id серий у плееров свои, поэтому цепочка читается в момент воспроизведения. Поток Fenix
     * приходит без карты качеств: плеер играет мастер адаптивно, а гео-блока у Fenix и Moon нет.
     */
    override suspend fun getStreams(target: AnimeOnTarget): AnimeOnStreamsResult {
        val episodes = api.getText(animeOnEpisodesUrl(target.titleKey, target.playerId, target.translationId, target.episode))
            ?: return AnimeOnStreamsResult.NotFound
        val episodeId = animeOnEpisodeId(episodes, target.episode) ?: return AnimeOnStreamsResult.NotFound
        val video = api.getText(animeOnVideoUrl(episodeId)) ?: return AnimeOnStreamsResult.NotFound
        val playerUrl = animeOnPlayerUrl(video) ?: return AnimeOnStreamsResult.NotFound
        return when {
            isMoonPlayerUrl(playerUrl) -> getMoonStreams(playerUrl)
            isFenixPlayerUrl(playerUrl) -> getFenixStreams(playerUrl, animeOnPlayerTrack(video))
            else -> getAshdiStreams(playerUrl)
        }
    }

    private suspend fun getAshdiStreams(playerUrl: String): AnimeOnStreamsResult =
        when (val result = ashdi.getStreams(playerUrl)) {
            is AshdiStreamsResult.Success -> AnimeOnStreamsResult.Success(AnimeOnStreams(result.streams.masterUrl, result.streams.qualities))
            AshdiStreamsResult.NotFound -> AnimeOnStreamsResult.NotFound
            AshdiStreamsResult.RegionBlocked -> AnimeOnStreamsResult.RegionBlocked
            is AshdiStreamsResult.Failed -> AnimeOnStreamsResult.Failed(result.reason)
        }

    private suspend fun getFenixStreams(playerUrl: String, track: String?): AnimeOnStreamsResult =
        when (val result = fenix.getStreams(playerUrl, track)) {
            is FenixStreamsResult.Success -> AnimeOnStreamsResult.Success(AnimeOnStreams(result.streams.masterUrl))
            FenixStreamsResult.NotFound -> AnimeOnStreamsResult.NotFound
            is FenixStreamsResult.Failed -> AnimeOnStreamsResult.Failed(result.reason)
        }

    private suspend fun getMoonStreams(playerUrl: String): AnimeOnStreamsResult =
        when (val result = moon.getStreams(playerUrl)) {
            is MoonStreamsResult.Success -> AnimeOnStreamsResult.Success(
                AnimeOnStreams(result.streams.url, result.streams.qualities.orEmpty(), result.streams.headers),
            )
            MoonStreamsResult.NotFound -> AnimeOnStreamsResult.NotFound
            is MoonStreamsResult.Failed -> AnimeOnStreamsResult.Failed(result.reason)
        }
}
