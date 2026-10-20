package su.afk.yummy.tv.feature.details.screenshots.utils

import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

fun AnimeScreenshot.screenshotLazyKey(index: Int): String =
    full?.takeIf { it.isNotBlank() }
        ?: small?.takeIf { it.isNotBlank() }
        ?: "empty:$index"

/** Адреса кадров для галереи: полный размер, иначе превью. */
fun List<AnimeScreenshot>.galleryUrls(): List<String> = mapNotNull { it.full ?: it.small }

/** Адрес кадра [index] для галереи; `null`, если кадра нет. */
fun List<AnimeScreenshot>.galleryUrlAt(index: Int): String? =
    getOrNull(index)?.let { it.full ?: it.small }
