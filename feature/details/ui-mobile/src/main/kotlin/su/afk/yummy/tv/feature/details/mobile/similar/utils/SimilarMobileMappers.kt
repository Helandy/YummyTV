package su.afk.yummy.tv.feature.details.mobile.similar.utils

import su.afk.yummy.tv.core.model.anime.AnimePoster

internal fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small

internal const val SIMILAR_SOURCE_PAGE_COUNT = 2
private const val SIMILAR_SOURCE_SITE_PAGE = 0
private const val SIMILAR_SOURCE_AI_PAGE = 1

internal fun Boolean.toSimilarSourcePage(): Int =
    if (this) SIMILAR_SOURCE_AI_PAGE else SIMILAR_SOURCE_SITE_PAGE

internal fun Int.toSimilarSourceFromAi(): Boolean =
    this == SIMILAR_SOURCE_AI_PAGE
