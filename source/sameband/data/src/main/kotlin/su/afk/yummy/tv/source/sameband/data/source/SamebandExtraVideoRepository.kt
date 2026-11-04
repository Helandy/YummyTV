package su.afk.yummy.tv.source.sameband.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.sameband.data.utils.samebandSlugFromUrl
import su.afk.yummy.tv.source.sameband.data.utils.toSourceUrl
import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget
import su.afk.yummy.tv.source.sameband.domain.usecase.ListSamebandEpisodesUseCase
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «Sameband» (дубляж). Наличие тайтла приходит из общей ручки наличия на сервере YummyTV, а число серий
 * сервер не знает (может быть `null`), поэтому серии берутся из плейлиста плеера на сайте. Ссылок на файлы здесь нет: HLS
 * забирается при открытии плеера.
 */
internal class SamebandExtraVideoRepository @Inject constructor(
    private val findAvailability: FindSourceAvailabilityUseCase,
    private val listEpisodes: ListSamebandEpisodesUseCase,
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
        val slug = samebandSlugFromUrl(availability.url) ?: availability.key

        return listEpisodes(slug).map { episode ->
            val url = SamebandTarget(slug, episode).toSourceUrl()
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
        const val SOURCE_ID = "sameband"
        const val PLAYER_NAME = "Sameband"

        /** Сайт — студия озвучки «Студийная Банда»: серии сливаются с её дорожкой из yani, значок берётся по этому имени. */
        const val DUBBING_NAME = "Студийная Банда"
    }
}
