package su.afk.yummy.tv.feature.details.screenshots.utils

import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

internal fun AnimeScreenshot.screenshotLazyKey(index: Int): String =
    full?.takeIf { it.isNotBlank() }
        ?: small?.takeIf { it.isNotBlank() }
        ?: "empty:$index"
