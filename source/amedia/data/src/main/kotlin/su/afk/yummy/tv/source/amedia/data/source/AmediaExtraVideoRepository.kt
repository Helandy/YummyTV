package su.afk.yummy.tv.source.amedia.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.amedia.data.utils.toSourceUrl
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.usecase.FindAmediaTitleUseCase
import su.afk.yummy.tv.source.amedia.domain.usecase.GetAmediaEpisodesUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «Amedia» с единственной озвучкой сайта amd.online. Страницу тайтла и её серии отдаёт сервер YummyTV (по MAL id
 * или названиям; на amd.online при открытии деталей клиент не ходит); без MAL id источника нет. Серия собственного плеера идёт адресом `source://amedia/…` (поток запрашивается при
 * открытии), а серия, вместо которой сайт вставил Rutube, — самой Rutube-ссылкой: её разбирает штатный Rutube-экстрактор.
 */
internal class AmediaExtraVideoRepository @Inject constructor(
    private val findTitle: FindAmediaTitleUseCase,
    private val getEpisodes: GetAmediaEpisodesUseCase,
) : ExtraVideoRepository {

    override val sourceId: String = "amedia"

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        val malId = details.malId ?: return emptyList()
        val lookup = AmediaLookup(
            malId = malId,
            title = details.title,
            otherTitles = details.otherTitles,
            year = details.year,
            type = details.type,
            episodes = details.episodes?.count,
        )
        val title = findTitle(lookup) ?: return emptyList()

        return getEpisodes(title).map { episode ->
            val url = when (val source = episode.source) {
                is AmediaEpisodeSource.Vod -> AmediaTarget(source.vodId).toSourceUrl()
                is AmediaEpisodeSource.Rutube -> source.embedUrl
            }
            AnimeVideo(
                // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                episode = episode.number.toString(),
                dubbing = DUBBING_NAME,
                player = PLAYER_NAME,
                playerId = null,
                iframeUrl = url,
                durationSeconds = null,
            )
        }
    }

    private companion object {
        const val PLAYER_NAME = "Amedia"
        const val DUBBING_NAME = "Amedia"
    }
}
