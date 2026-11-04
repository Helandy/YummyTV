package su.afk.yummy.tv.source.studiomir.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import su.afk.yummy.tv.source.studiomir.data.utils.toSourceUrl
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget
import javax.inject.Inject

/**
 * Добавляет балансер «Studio MIR» (озвучка Творческой студии МИР). Наличие тайтла и число серий приходят из общей ручки наличия
 * на сервере YummyTV (одним запросом на все балансеры). Ссылок на файлы здесь нет: плейлист серий забирается со страницы тайтла
 * при открытии плеера.
 */
internal class StudioMirExtraVideoRepository @Inject constructor(
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
            val url = StudioMirTarget(availability.key, episode).toSourceUrl()
            AnimeVideo(
                // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                episode = episode.toString(),
                dubbing = DUBBING_NAME,
                player = PLAYER_NAME,
                playerId = null,
                iframeUrl = url,
                durationSeconds = null,
            )
        }
    }

    private companion object {
        const val SOURCE_ID = "studiomir"
        const val PLAYER_NAME = "Studio MIR"

        /** Так озвучку называют другие балансеры (Kodik и др.): строки склеиваются, а «Studio MIR» остаётся меткой источника. */
        const val DUBBING_NAME = "Творческая студия МИР"
    }
}
