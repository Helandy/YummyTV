package su.afk.yummy.tv.feature.details.mapper

import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

private const val FIRST_EPISODE = "1"

/** Стабильный ключ кэша картинок карточки: тайтл и позиция в ряду, не зависит от источника ссылки. */
private fun screenshotCacheKey(animeId: Int, index: Int): String = "screenshot_v1:$animeId:$index"

/**
 * Скриншоты деталки из двух источников по приоритету: кадры серии 1 из TMDB, скриншоты Yani.
 *
 * Число карточек задаёт TMDB, а у каждой карточки своя цепочка ссылок: на i-й позиции TMDB, если она не загрузится
 * (CDN заблокирован), то i-й скриншот Yani ([AnimeScreenshot.fallbackUrls]). Все ссылки карточки пишутся в кэш картинок
 * под одним стабильным ключом ([AnimeScreenshot.cacheKey]). Если кадров TMDB нет, возвращаются скриншоты Yani как есть.
 */
internal fun mergeScreenshotSources(
    animeId: Int,
    frames: AnimeEpisodeFrames,
    yani: List<AnimeScreenshot>,
): List<AnimeScreenshot> {
    if (frames.tmdbStillUrls.isEmpty()) return yani
    return frames.tmdbStillUrls.indices.map { index ->
        val yaniScreenshot = yani.getOrNull(index)
        val urls = listOfNotNull(
            frames.tmdbStillUrls.getOrNull(index),
            yaniScreenshot?.url,
        ).filter { it.isNotBlank() }.distinct()
        AnimeScreenshot(
            id = yaniScreenshot?.id,
            episode = FIRST_EPISODE,
            url = urls.firstOrNull(),
            fallbackUrls = urls.drop(1),
            cacheKey = screenshotCacheKey(animeId, index),
        )
    }
}
