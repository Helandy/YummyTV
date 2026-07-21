package su.afk.yummy.tv.feature.details.mobile.viewingorder.utils

import su.afk.yummy.tv.core.model.anime.AnimePoster

internal fun AnimePoster?.bestUrl(): String? =
    this?.mega ?: this?.fullsize ?: this?.big ?: this?.medium ?: this?.small
