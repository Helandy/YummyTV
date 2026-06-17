package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.domain.library.model.LibraryPoster

internal fun AnimePoster.toLibraryPoster(): LibraryPoster =
    LibraryPoster(
        small = small,
        medium = medium,
        big = big,
        fullsize = fullsize,
        mega = mega,
    )
