package su.afk.yummy.tv.feature.details.similar.utils

import su.afk.yummy.tv.domain.anime.model.AnimePoster

internal fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
