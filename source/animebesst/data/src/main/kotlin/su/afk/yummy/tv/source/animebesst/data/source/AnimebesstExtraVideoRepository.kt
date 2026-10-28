package su.afk.yummy.tv.source.animebesst.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.animebesst.data.utils.animebesstTitleRef
import su.afk.yummy.tv.source.animebesst.data.utils.toSourceUrl
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «Animebesst» (anime1.best). Наличие тайтла и число серий приходят из общей ручки наличия на сервере YummyTV:
 * `url` — страница тайтла, `mirrors` — та же страница на зеркалах (для сетей, где сайт закрыт). Хосты уходят в адрес серии, чтобы
 * при воспроизведении страницу можно было открыть через зеркало. Ссылок на файлы здесь нет: поток берётся со страницы плеера
 * при открытии.
 */
internal class AnimebesstExtraVideoRepository @Inject constructor(
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
        val (slug, hosts) = animebesstTitleRef(availability.url, availability.mirrors) ?: return emptyList()

        return (1..availability.episodesAvailable).map { episode ->
            val url = AnimebesstTarget(slug, episode, hosts).toSourceUrl()
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
        const val SOURCE_ID = "animebesst"
        const val PLAYER_NAME = "Animebesst"
    }
}
