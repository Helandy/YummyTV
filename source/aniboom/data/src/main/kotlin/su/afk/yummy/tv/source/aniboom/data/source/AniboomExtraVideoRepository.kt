package su.afk.yummy.tv.source.aniboom.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.aniboom.data.utils.toSourceUrl
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.usecase.FindAniboomTitleUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «AniBoom» с собственными озвучками. Тайтл ищет сервер YummyTV по MAL id и названиям; без MAL id
 * источника нет. Ссылок на файлы здесь нет: поток берётся из эмбеда при открытии плеера. Число серий приходит с сервера уже без
 * серии-заглушки (сервер читает анонс следующей серии со страницы animego).
 */
internal class AniboomExtraVideoRepository @Inject constructor(
    private val findTitle: FindAniboomTitleUseCase,
) : ExtraVideoRepository {

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        val malId = details.malId ?: return emptyList()
        val lookup = AniboomLookup(
            malId = malId,
            title = details.title,
            otherTitles = details.otherTitles,
            year = details.year,
            type = details.type,
            episodes = details.episodes?.count,
        )
        val title = findTitle(lookup) ?: return emptyList()

        return title.dubbings.flatMap { dubbing ->
            (1..title.episodesAvailable).map { episode ->
                val url = AniboomTarget(title.aniboomId, episode, dubbing.translation).toSourceUrl()
                AnimeVideo(
                    // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                    id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                    episode = episode.toString(),
                    dubbing = dubbing.title,
                    player = PLAYER_NAME,
                    playerId = null,
                    iframeUrl = url,
                    durationSeconds = null,
                )
            }
        }
    }

    private companion object {
        const val PLAYER_NAME = "AniBoom"
    }
}
