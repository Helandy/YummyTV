package su.afk.yummy.tv.source.mikai.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import su.afk.yummy.tv.source.mikai.data.utils.mikaiFenixSourceUrl
import su.afk.yummy.tv.source.mikai.data.utils.mikaiFenixToken
import su.afk.yummy.tv.source.mikai.data.utils.mikaiMoonId
import su.afk.yummy.tv.source.mikai.data.utils.mikaiMoonSourceUrl
import su.afk.yummy.tv.source.mikai.data.utils.mikaiSourceUrl
import su.afk.yummy.tv.source.mikai.data.utils.mikaiVodId
import javax.inject.Inject

/**
 * Добавляет балансеры «Ashdi», «Fenix» и «Moon» от агрегатора Mikai (украинская озвучка и субтитры). Наличие тайтла приходит из общей ручки наличия на сервере YummyTV:
 * вариант — команда на одном провайдере плеера (`key` = `провайдер:id команды`, `title` — имя команды), у серии `id` — запись
 * в плеере (у Fenix — токен серии), а у Moon `id` пуст и берётся из адреса страницы плеера (`url`). Играются провайдеры Ashdi, Fenix и Moon,
 * остальные варианты пропускаются; озвучки-субтитры Fenix и Moon тоже: субтитры их плееров приложение не подключает, и серия играла бы
 * без текста. Ссылок на файлы здесь нет: поток берётся со страницы плеера при открытии.
 */
internal class MikaiExtraVideoRepository @Inject constructor(
    private val findAvailability: FindSourceAvailabilityUseCase,
    private val messages: MikaiMessages,
) : ExtraVideoRepository {

    override val sourceId: String = SOURCE_ID

    /** Выше AnimeON: у Mikai есть прямые id серий плеера, а те же Ashdi/Fenix/Moon у AnimeON идут через его сайт. */
    override val priority: Int = 1

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
            .filter { isAshdi(it.key) || (!it.isSubs && (isFenix(it.key) || isMoon(it.key))) }
            .distinctBy { it.key }
            .flatMap { variant ->
                val team = variant.title ?: return@flatMap emptyList()
                val withSubs = if (variant.isSubs) team + messages.subsSuffix else team
                val player = when {
                    isFenix(variant.key) -> FENIX_PLAYER
                    isMoon(variant.key) -> MOON_PLAYER
                    else -> ASHDI_PLAYER
                }
                variant.episodes.mapNotNull { episode ->
                    val url = when (player) {
                        FENIX_PLAYER -> mikaiFenixSourceUrl(mikaiFenixToken(episode.id, episode.url) ?: return@mapNotNull null)
                        MOON_PLAYER -> mikaiMoonSourceUrl(mikaiMoonId(episode.url) ?: return@mapNotNull null)
                        else -> mikaiSourceUrl(mikaiVodId(episode.id, episode.url) ?: return@mapNotNull null)
                    }
                    AnimeVideo(
                        // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                        id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                        episode = episode.number.toString(),
                        dubbing = withSubs,
                        player = player,
                        playerId = null,
                        iframeUrl = url,
                        durationSeconds = null,
                    )
                }
            }
    }

    private fun isAshdi(key: String) = key.startsWith(ASHDI_KEY_PREFIX, ignoreCase = true)

    private fun isFenix(key: String) = key.startsWith(FENIX_KEY_PREFIX, ignoreCase = true)

    private fun isMoon(key: String) = key.startsWith(MOON_KEY_PREFIX, ignoreCase = true)

    private companion object {
        const val SOURCE_ID = "mikai"
        const val ASHDI_PLAYER = "Ashdi"
        const val FENIX_PLAYER = "Fenix"
        const val MOON_PLAYER = "Moon"
        const val ASHDI_KEY_PREFIX = "ashdi:"
        const val FENIX_KEY_PREFIX = "fenix:"
        const val MOON_KEY_PREFIX = "moonanime:"
    }
}
