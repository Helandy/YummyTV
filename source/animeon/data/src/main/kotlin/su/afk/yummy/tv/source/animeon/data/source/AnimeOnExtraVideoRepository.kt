package su.afk.yummy.tv.source.animeon.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.animeon.data.utils.ANIMEON_ASHDI_PLAYER
import su.afk.yummy.tv.source.animeon.data.utils.ANIMEON_FENIX_PLAYER
import su.afk.yummy.tv.source.animeon.data.utils.toSourceUrl
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «AnimeON» (украинская озвучка и субтитры). Наличие тайтла и варианты приходят из общей ручки наличия на
 * сервере YummyTV: `key` тайтла — id на сайте, вариант — озвучка в одном плеере (`translationId:playerId`, `title` — «озвучка
 * (плеер)»). Играются плееры Ashdi и Fenix, остальные варианты пропускаются. Ссылок на файлы здесь нет: поток берётся
 * со страницы плеера при открытии.
 */
internal class AnimeOnExtraVideoRepository @Inject constructor(
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
                val title = variant.title ?: return@flatMap emptyList()
                val suffix = PLAYER_SUFFIXES.firstOrNull { title.endsWith(it) } ?: return@flatMap emptyList()
                val (translationId, playerId) = variant.key.split(':').let { parts ->
                    if (parts.size != 2) return@flatMap emptyList()
                    val ids = parts.map { it.toIntOrNull()?.takeIf { id -> id > 0 } ?: return@flatMap emptyList() }
                    ids[0] to ids[1]
                }
                // Озвучка в Fenix остаётся с пометкой плеера: часто та же есть и в Ashdi, а эти варианты нужно отличать.
                val dubbing = if (suffix == FENIX_SUFFIX) title else title.removeSuffix(suffix)
                variant.episodes.map { episode ->
                    val url = AnimeOnTarget(availability.key, translationId, playerId, episode.number).toSourceUrl()
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
        const val SOURCE_ID = "animeon"
        const val PLAYER_NAME = "AnimeON"
        const val FENIX_SUFFIX = " ($ANIMEON_FENIX_PLAYER)"
        val PLAYER_SUFFIXES = listOf(" ($ANIMEON_ASHDI_PLAYER)", FENIX_SUFFIX)
    }
}
