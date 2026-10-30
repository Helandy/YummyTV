package su.afk.yummy.tv.feature.details.screenshots.utils

import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

fun AnimeScreenshot.screenshotLazyKey(index: Int): String =
    url?.takeIf { it.isNotBlank() } ?: "empty:$index"

/** Ссылки карточки по приоритету для `FallbackAsyncImage`: следующая берётся, если предыдущая не загрузилась. */
fun AnimeScreenshot.imageModels(): List<String> =
    listOfNotNull(url?.takeIf { it.isNotBlank() }) + fallbackUrls

/** Адреса кадров для галереи. */
fun List<AnimeScreenshot>.galleryUrls(): List<String> = mapNotNull { it.url }

/** Адрес кадра [index] для галереи; `null`, если кадра нет. */
fun List<AnimeScreenshot>.galleryUrlAt(index: Int): String? =
    getOrNull(index)?.url
