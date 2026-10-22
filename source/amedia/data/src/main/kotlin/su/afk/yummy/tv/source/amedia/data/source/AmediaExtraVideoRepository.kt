package su.afk.yummy.tv.source.amedia.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.amedia.data.utils.toSourceUrl
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.usecase.FindAmediaTitleUseCase
import su.afk.yummy.tv.source.amedia.domain.usecase.GetAmediaEpisodesUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «Amedia» с единственной озвучкой сайта amd.online. Страницу тайтла находит поиск по названиям с
 * подтверждением MAL id; без MAL id источника нет. Поток запрашивается при открытии плеера, поэтому здесь только адреса серий.
 */
internal class AmediaExtraVideoRepository @Inject constructor(
    private val findTitle: FindAmediaTitleUseCase,
    private val getEpisodes: GetAmediaEpisodesUseCase,
) : ExtraVideoRepository {

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        val malId = details.malId ?: return emptyList()
        val title = findTitle(AmediaLookup(malId, details.title, details.otherTitles)) ?: return emptyList()

        return getEpisodes(title).map { episode ->
            val url = AmediaTarget(episode.vodId).toSourceUrl()
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
