package su.afk.yummy.tv.domain.library.utils

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import java.util.Locale

private const val MIN_VALID_SCORE = 1
private const val MAX_VALID_SCORE = 10

/** Преобразует статус из JSON или `<shiki_status>` Shikimori в [UserAnimeList]. */
fun String?.toUserAnimeListFromShikimori(): UserAnimeList? =
    when (this?.trim()?.lowercase(Locale.ROOT)) {
        "watching", "rewatching" -> UserAnimeList.WATCHING
        "planned" -> UserAnimeList.PLANNED
        "completed" -> UserAnimeList.COMPLETED
        "on_hold" -> UserAnimeList.POSTPONED
        "dropped" -> UserAnimeList.DROPPED
        else -> null
    }

/**
 * Преобразует статус `<my_status>` из XML MyAnimeList в [UserAnimeList]. Поддерживает как
 * строковые значения современного экспорта, так и числовые коды старого формата MAL.
 */
fun String?.toUserAnimeListFromMal(): UserAnimeList? {
    val normalized = this?.trim()?.lowercase(Locale.ROOT) ?: return null
    return when (normalized) {
        "watching", "1" -> UserAnimeList.WATCHING
        "completed", "2" -> UserAnimeList.COMPLETED
        "on-hold", "on hold", "on_hold", "3" -> UserAnimeList.POSTPONED
        "dropped", "4" -> UserAnimeList.DROPPED
        "plan to watch", "plan_to_watch", "planned", "6" -> UserAnimeList.PLANNED
        else -> null
    }
}

/** Строковое представление статуса для `<my_status>` в XML MyAnimeList. */
fun UserAnimeList.toMalStatus(): String =
    when (this) {
        UserAnimeList.WATCHING -> "Watching"
        UserAnimeList.COMPLETED -> "Completed"
        UserAnimeList.POSTPONED -> "On-Hold"
        UserAnimeList.DROPPED -> "Dropped"
        UserAnimeList.PLANNED -> "Plan to Watch"
    }

/** Строковое представление статуса для поля `status` / `<shiki_status>` Shikimori. */
fun UserAnimeList.toShikimoriStatus(): String =
    when (this) {
        UserAnimeList.WATCHING -> "watching"
        UserAnimeList.PLANNED -> "planned"
        UserAnimeList.COMPLETED -> "completed"
        UserAnimeList.POSTPONED -> "on_hold"
        UserAnimeList.DROPPED -> "dropped"
    }

/** Нормализует пользовательскую оценку в диапазон `1..10`; `0` и прочие значения дают `null`. */
fun Int?.normalizedExternalScore(): Int? =
    this?.takeIf { it in MIN_VALID_SCORE..MAX_VALID_SCORE }

/** Преобразует `type.alias` каталога yani в значение `<series_type>` формата MyAnimeList. */
fun String?.toMalSeriesType(): String =
    when (this?.trim()?.lowercase(Locale.ROOT)) {
        "tv" -> "TV"
        "movie", "shortfilm" -> "Movie"
        "ova" -> "OVA"
        "ona" -> "ONA"
        "special", "tv_special" -> "Special"
        "music" -> "Music"
        else -> "Unknown"
    }
