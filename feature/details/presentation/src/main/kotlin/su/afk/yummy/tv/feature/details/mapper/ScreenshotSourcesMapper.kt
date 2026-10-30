package su.afk.yummy.tv.feature.details.mapper

import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

private const val FIRST_EPISODE = "1"

/** Стабильный ключ кэша картинок карточки: тайтл и позиция в ряду, не зависит от источника ссылки. */
private fun screenshotCacheKey(animeId: Int, index: Int): String = "screenshot_v1:$animeId:$index"

/**
 * Скриншоты деталки из трёх источников по приоритету: кадры серии 1 из TMDB, кадры Tenrai, скриншоты Yani.
 *
 * Число карточек задаёт первый непустой источник, а у каждой карточки своя цепочка ссылок: на i-й позиции TMDB, если
 * она не загрузится (CDN заблокирован), то i-й кадр Tenrai, затем i-й скриншот Yani ([AnimeScreenshot.fallbackUrls]).
 * Все ссылки карточки пишутся в кэш картинок под одним стабильным ключом ([AnimeScreenshot.cacheKey]). Если кадров нет
 * вовсе, возвращаются скриншоты Yani как есть.
 */
internal fun mergeScreenshotSources(
    animeId: Int,
    frames: AnimeEpisodeFrames,
    yani: List<AnimeScreenshot>,
): List<AnimeScreenshot> {
    val primary = listOf(frames.tmdbStillUrls, frames.tenraiFrameUrls).firstOrNull { it.isNotEmpty() }
        ?: return yani
    return primary.indices.map { index ->
        val yaniScreenshot = yani.getOrNull(index)
        val urls = listOfNotNull(
            frames.tmdbStillUrls.getOrNull(index),
            frames.tenraiFrameUrls.getOrNull(index),
            yaniScreenshot?.url,
        ).filter { it.isNotBlank() }.distinct()
        AnimeScreenshot(
            id = yaniScreenshot?.id,
            episode = if (frames.tmdbStillUrls.isNotEmpty()) FIRST_EPISODE else (index + 1).toString(),
            url = urls.firstOrNull(),
            fallbackUrls = urls.drop(1),
            cacheKey = screenshotCacheKey(animeId, index),
        )
    }
}
