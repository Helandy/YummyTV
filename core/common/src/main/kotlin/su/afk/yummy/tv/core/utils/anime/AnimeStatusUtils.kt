package su.afk.yummy.tv.core.utils.anime

import java.util.Locale

private val RELEASED_STATUSES = setOf("released", "вышел")
private val ONGOING_STATUSES = setOf("ongoing", "онгоинг")

/** Тайтл вышел полностью (статус yani приходит как `released` или «вышел»). */
fun String?.isReleasedAnimeStatus(): Boolean =
    this?.trim()?.lowercase(Locale.ROOT) in RELEASED_STATUSES

/** Тайтл выходит сейчас (статус yani приходит как `ongoing` или «онгоинг»). */
fun String?.isOngoingAnimeStatus(): Boolean =
    this?.trim()?.lowercase(Locale.ROOT) in ONGOING_STATUSES
