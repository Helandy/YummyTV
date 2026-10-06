package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot

/** Минимальные детали тайтла для тестов: всё необязательное пустое. */
internal fun animeDetails(
    id: Int = 1,
    title: String = "title",
    screenshots: List<AnimeScreenshot> = emptyList(),
) = AnimeDetails(
    id = id,
    animeUrl = "url",
    title = title,
    description = "",
    poster = null,
    rating = AnimeRating(null, null, null, null, null),
    genres = emptyList(),
    year = null,
    ageRating = null,
    views = null,
    status = null,
    type = null,
    episodes = null,
    otherTitles = emptyList(),
    creators = emptyList(),
    studios = emptyList(),
    viewingOrder = emptyList(),
    screenshots = screenshots,
)
