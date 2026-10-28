package su.afk.yummy.tv.source.dreamerscast.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import su.afk.yummy.tv.source.dreamerscast.data.utils.dreamerscastSlug
import su.afk.yummy.tv.source.dreamerscast.data.utils.toSourceUrl
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget
import javax.inject.Inject

/**
 * Добавляет балансер «Dream Cast» (студия озвучки, dreamerscast.com). Наличие релиза и число озвученных серий приходят из общей
 * ручки наличия на сервере YummyTV: `key` — `{id}-{slug}`, `url` — страница релиза. Ссылок на файлы здесь нет: HLS берётся из
 * плеера страницы при открытии.
 */
internal class DreamerscastExtraVideoRepository @Inject constructor(
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
        val slug = dreamerscastSlug(availability.url) ?: return emptyList()

        return (1..availability.episodesAvailable).map { episode ->
            val url = DreamerscastTarget(slug, episode).toSourceUrl()
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
        const val SOURCE_ID = "dreamerscast"
        const val PLAYER_NAME = "Dream Cast"
    }
}
