package su.afk.yummy.tv.source.anistar.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.anistar.data.utils.toSourceUrl
import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.usecase.FindAnistarTitleUseCase
import su.afk.yummy.tv.source.anistar.domain.usecase.GetAnistarEpisodesUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «AniStar» с единственной озвучкой сайта. Тайтл и номера серий отдаёт сервер YummyTV (ищет по MAL id и названиям, плеер-страницу читает сам с кэшем); без MAL id
 * источника нет. Ссылок на файлы здесь нет: у них токен и срок жизни, поэтому
 * поток запрашивается при открытии плеера.
 */
internal class AnistarExtraVideoRepository @Inject constructor(
    private val findTitle: FindAnistarTitleUseCase,
    private val getEpisodes: GetAnistarEpisodesUseCase,
) : ExtraVideoRepository {

    override val sourceId: String = "anistar"

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        val malId = details.malId ?: return emptyList()
        val lookup = AnistarLookup(
            malId = malId,
            title = details.title,
            otherTitles = details.otherTitles,
            year = details.year,
            type = details.type,
            episodes = details.episodes?.count,
        )
        val title = findTitle(lookup) ?: return emptyList()

        return getEpisodes(title).map { episode ->
            val url = AnistarTarget(title.newsId, episode.number).toSourceUrl()
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
        const val PLAYER_NAME = "AniStar"
        const val DUBBING_NAME = "AniStar"
    }
}
