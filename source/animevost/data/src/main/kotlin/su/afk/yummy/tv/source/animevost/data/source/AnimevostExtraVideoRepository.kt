package su.afk.yummy.tv.source.animevost.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.animevost.data.utils.animevostTitleRef
import su.afk.yummy.tv.source.animevost.data.utils.toSourceUrl
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «AnimeVost» (озвучка студии AnimeVost). Наличие тайтла и число озвученных серий приходят из общей ручки
 * наличия на сервере YummyTV: `key` — `{id}-{slug}`, раздел берётся из адреса страницы. Ссылок на файлы здесь нет: mp4 берётся
 * из плеера серии при открытии.
 */
internal class AnimevostExtraVideoRepository @Inject constructor(
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
        val (typePath, slug) = animevostTitleRef(availability.url) ?: return emptyList()

        return (1..availability.episodesAvailable).map { episode ->
            val url = AnimevostTarget(typePath, slug, episode).toSourceUrl()
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
        const val SOURCE_ID = "animevost"
        const val PLAYER_NAME = "AnimeVost"
    }
}
