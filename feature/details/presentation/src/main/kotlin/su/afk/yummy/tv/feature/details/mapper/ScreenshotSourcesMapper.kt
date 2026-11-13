package su.afk.yummy.tv.feature.details.mapper

import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

private const val FIRST_EPISODE = "1"

/** Сколько карточек добираем, если TMDB дал единственный кадр. */
private const val SPARSE_TMDB_TARGET_COUNT = 6

/** Стабильный ключ кэша картинок карточки: тайтл и позиция в ряду, не зависит от источника ссылки. */
private fun screenshotCacheKey(animeId: Int, index: Int): String = "screenshot_v1:$animeId:$index"

/**
 * Скриншоты деталки из трёх источников по приоритету: кадры серии 1 из TMDB, их копия на нашем хосте, скриншоты Yani.
 *
 * Число карточек задаёт TMDB, а у каждой карточки своя цепочка ссылок: на i-й позиции TMDB, если она не загрузится
 * (CDN заблокирован), то её копия на нашем хосте, затем i-й скриншот Yani ([AnimeScreenshot.fallbackUrls]). Все ссылки карточки пишутся в кэш картинок
 * под одним стабильным ключом ([AnimeScreenshot.cacheKey]). Если кадров TMDB нет, возвращаются скриншоты Yani как есть.
 * Если TMDB дал ровно один кадр, ряд добирается до [SPARSE_TMDB_TARGET_COUNT] карточек скриншотами Yani (других серий).
 */
internal fun mergeScreenshotSources(
    animeId: Int,
    frames: AnimeEpisodeFrames,
    yani: List<AnimeScreenshot>,
): List<AnimeScreenshot> {
    if (frames.tmdbStillUrls.isEmpty()) return yani
    val tmdbCards = frames.tmdbStillUrls.indices.map { index ->
        val yaniScreenshot = yani.getOrNull(index)
        val urls = listOfNotNull(
            frames.tmdbStillUrls.getOrNull(index),
            frames.tmdbStillMirrorUrls.getOrNull(index),
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
    if (frames.tmdbStillUrls.size != 1) return tmdbCards
    // Yani[0] уже запасная ссылка первой карточки, добираем со следующих.
    val extras = yani.drop(tmdbCards.size)
        .filter { !it.url.isNullOrBlank() }
        .take(SPARSE_TMDB_TARGET_COUNT - tmdbCards.size)
        .mapIndexed { i, shot -> shot.copy(cacheKey = screenshotCacheKey(animeId, tmdbCards.size + i)) }
    return tmdbCards + extras
}
