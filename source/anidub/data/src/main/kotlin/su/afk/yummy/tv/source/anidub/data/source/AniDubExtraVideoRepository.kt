package su.afk.yummy.tv.source.anidub.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.anidub.data.utils.toSourceUrl
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «AniDub» (дубляж студии АниДаб). Наличие тайтла и число озвученных серий приходят из общей ручки наличия на
 * сервере YummyTV (одним запросом на все балансеры); сервер отдаёт только тайтлы с плеером ladonyvesna. Ссылок на файлы здесь нет:
 * подписанный HLS забирается со страницы тайтла при открытии плеера.
 */
internal class AniDubExtraVideoRepository @Inject constructor(
    private val findAvailability: FindSourceAvailabilityUseCase,
) : ExtraVideoRepository {

    override val sourceId: String = SOURCE_ID

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        val lookup = AvailabilityLookup(
            malId = details.malId,
            title = details.title,
            otherTitles = details.otherTitles,
            year = details.year,
            type = details.type,
            episodes = details.episodes?.count,
        )
        val availability = findAvailability(SOURCE_ID, lookup) ?: return emptyList()

        return (1..availability.episodesAvailable).map { episode ->
            val url = AniDubTarget(availability.key, episode).toSourceUrl()
            AnimeVideo(
                // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                episode = episode.toString(),
                dubbing = PLAYER_NAME,
                player = PLAYER_NAME,
                playerId = null,
                iframeUrl = url,
                durationSeconds = null,
            )
        }
    }

    private companion object {
        const val SOURCE_ID = "anidub"
        const val PLAYER_NAME = "AniDub"
    }
}
