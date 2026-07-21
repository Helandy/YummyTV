package su.afk.yummy.tv.feature.top.mobile.utils

import su.afk.yummy.tv.domain.top.model.AnimeTopType

internal val topMobileTypes: List<AnimeTopType>
    get() = AnimeTopType.entries

internal fun AnimeTopType.toTopTypePage(): Int =
    topMobileTypes.indexOf(this).coerceAtLeast(0)

internal fun Int.toTopType(): AnimeTopType =
    topMobileTypes.getOrElse(this) { AnimeTopType.TV }

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
