package su.afk.yummy.tv.source.mikai.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import su.afk.yummy.tv.source.mikai.data.utils.mikaiFenixSourceUrl
import su.afk.yummy.tv.source.mikai.data.utils.mikaiFenixToken
import su.afk.yummy.tv.source.mikai.data.utils.mikaiSourceUrl
import su.afk.yummy.tv.source.mikai.data.utils.mikaiVodId
import javax.inject.Inject

/**
 * Добавляет балансер «Mikai» (украинская озвучка и субтитры). Наличие тайтла приходит из общей ручки наличия на сервере YummyTV:
 * вариант — команда на одном провайдере плеера (`key` = `провайдер:id команды`, `title` — имя команды), у серии `id` — запись
 * в плеере (у Fenix — токен серии). Играются провайдеры Ashdi и Fenix, остальные варианты пропускаются; озвучки-субтитры Fenix
 * тоже: субтитры его плеера приложение не подключает, и серия играла бы без текста. Ссылок на файлы здесь нет: поток берётся
 * со страницы плеера при открытии.
 */
internal class MikaiExtraVideoRepository @Inject constructor(
    private val findAvailability: FindSourceAvailabilityUseCase,
    private val messages: MikaiMessages,
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
            .filter { isAshdi(it.key) || (isFenix(it.key) && !it.isSubs) }
            .distinctBy { it.key }
            .flatMap { variant ->
                val team = variant.title ?: return@flatMap emptyList()
                val withSubs = if (variant.isSubs) team + messages.subsSuffix else team
                // Озвучка в Fenix остаётся с пометкой плеера: та же команда часто есть и в Ashdi, а эти варианты нужно отличать.
                val dubbing = if (isFenix(variant.key)) "$withSubs$FENIX_DUBBING_SUFFIX" else withSubs
                variant.episodes.mapNotNull { episode ->
                    val url = if (isFenix(variant.key)) {
                        mikaiFenixSourceUrl(mikaiFenixToken(episode.id, episode.url) ?: return@mapNotNull null)
                    } else {
                        mikaiSourceUrl(mikaiVodId(episode.id, episode.url) ?: return@mapNotNull null)
                    }
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

    private fun isAshdi(key: String) = key.startsWith(ASHDI_KEY_PREFIX, ignoreCase = true)

    private fun isFenix(key: String) = key.startsWith(FENIX_KEY_PREFIX, ignoreCase = true)

    private companion object {
        const val SOURCE_ID = "mikai"
        const val PLAYER_NAME = "Mikai"
        const val ASHDI_KEY_PREFIX = "ashdi:"
        const val FENIX_KEY_PREFIX = "fenix:"
        const val FENIX_DUBBING_SUFFIX = " (Fenix)"
    }
}
