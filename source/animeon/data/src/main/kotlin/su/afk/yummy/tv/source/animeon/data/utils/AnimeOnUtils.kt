package su.afk.yummy.tv.source.animeon.data.utils

import kotlinx.serialization.json.Json
import su.afk.yummy.tv.source.animeon.data.dto.AnimeOnEpisodesDto
import su.afk.yummy.tv.source.animeon.data.dto.AnimeOnVideoUrlDto

internal const val ANIMEON_BASE_URL = "https://animeon.club"

/** Имя плеера в названии варианта с сервера (`FanVoxUA (Ashdi)`): играются Ashdi и Fenix, остальные (Moon) приложением пока нет. */
internal const val ANIMEON_ASHDI_PLAYER = "Ashdi"
internal const val ANIMEON_FENIX_PLAYER = "Fenix"

private const val FENIX_PAGE_PREFIX = "https://fenixplay.xyz/"

private val json = Json { ignoreUnknownKeys = true }

/** Страница серий плеера тайтла, начиная с серии [episode] (`skip` считается с нуля). */
internal fun animeOnEpisodesUrl(titleKey: String, playerId: Int, translationId: Int, episode: Int): String =
    "$ANIMEON_BASE_URL/api/player/$titleKey/episodes?take=1&skip=${episode - 1}&playerId=$playerId&translationId=$translationId"

internal fun animeOnVideoUrl(episodeId: Int): String = "$ANIMEON_BASE_URL/api/player/$episodeId/episode"

/** Id серии с номером [episode]; `null`, если страница пришла с другой серией (запрошенной нет). */
internal fun animeOnEpisodeId(body: String, episode: Int): Int? =
    json.decodeFromString<AnimeOnEpisodesDto>(body).episodes.firstOrNull { it.episode == episode }?.id

/** Адрес страницы плеера из ответа сайта; `null`, если ответ пуст. Какой это плеер, решает разбор плеера. */
internal fun animeOnPlayerUrl(body: String): String? = json.decodeFromString<AnimeOnVideoUrlDto>(body).videoUrl?.takeIf { it.isNotBlank() }

/** Озвучка, которую сайт выбрал на странице плеера; `null`, если ответ её не называет. */
internal fun animeOnPlayerTrack(body: String): String? =
    json.decodeFromString<AnimeOnVideoUrlDto>(body).selection?.track?.takeIf { it.isNotBlank() }

/** Страница плеера Fenix (`https://fenixplay.xyz/embed/…`), а не Ashdi. */
internal fun isFenixPlayerUrl(url: String): Boolean = url.startsWith(FENIX_PAGE_PREFIX)
