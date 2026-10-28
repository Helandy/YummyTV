package su.afk.yummy.tv.source.aniboom.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.aniboom.data.utils.toSourceUrl
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «AniBoom» с собственными озвучками. Тайтл ищет сервер YummyTV по названиям (общая ручка наличия, одним
 * запросом на все балансеры): ключ ответа — id тайтла в эмбеде, варианты — озвучки (`key` — id озвучки в эмбеде, `title` —
 * название) со своими сериями. Ссылок на файлы здесь нет: поток берётся из эмбеда при открытии плеера. Число серий приходит
 * с сервера уже без серии-заглушки (сервер читает анонс следующей серии со страницы animego).
 */
internal class AniboomExtraVideoRepository @Inject constructor(
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

        return availability.variants
            .distinctBy { it.key }
            .flatMap { variant ->
                val translation = variant.key.toIntOrNull()?.takeIf { it > 0 } ?: return@flatMap emptyList()
                val dubbing = variant.title ?: return@flatMap emptyList()
                variant.episodes.map { episode ->
                    val url = AniboomTarget(availability.key, episode.number, translation).toSourceUrl()
                    AnimeVideo(
                        // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                        id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                        episode = episode.number.toString(),
                        dubbing = dubbing,
                        player = PLAYER_NAME,
                        playerId = null,
                        iframeUrl = url,
                        durationSeconds = null,
                    )
                }
            }
    }

    private companion object {
        const val SOURCE_ID = "aniboom"
        const val PLAYER_NAME = "AniBoom"
    }
}
