package su.afk.yummy.tv.domain.library.utils

import su.afk.yummy.tv.core.model.error.isTooManyRequests
import su.afk.yummy.tv.core.model.error.retryAfterMillisOrNull
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.ExportableLibraryAnime
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ResolvedExternalAnime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EXPORT_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US).withZone(ZoneOffset.UTC)

private val SECONDARY_EDITION_MARKERS = listOf(
    "режиссерская версия",
    "режиссёрская версия",
    "режисерська версія",
    "director's cut",
    "directors cut",
)

private const val BASE_RETRY_DELAY_MS = 1_000L
private const val RATE_LIMIT_BASE_DELAY_MS = 2_500L
private const val MAX_BACKOFF_DELAY_MS = 30_000L

/**
 * Схлопывает дубликаты одного и того же `externalId` в файле импорта: берёт последний статус,
 * сохраняя непустое название, оценку и максимальное число просмотренных серий.
 */
fun List<ExternalListEntry>.deduplicateExternalEntries(): List<ExternalListEntry> {
    if (size <= 1) return this
    val merged = linkedMapOf<Int, ExternalListEntry>()
    forEach { entry ->
        val previous = merged[entry.externalId]
        merged[entry.externalId] = if (previous == null) {
            entry
        } else {
            entry.copy(
                title = entry.title.ifBlank { previous.title },
                score = entry.score ?: previous.score,
                watchedEpisodes = maxOf(entry.watchedEpisodes, previous.watchedEpisodes),
                rewatches = maxOf(entry.rewatches, previous.rewatches),
                seriesType = entry.seriesType ?: previous.seriesType,
                totalEpisodes = maxOf(entry.totalEpisodes, previous.totalEpisodes),
            )
        }
    }
    return merged.values.toList()
}

/**
 * Выбирает основной тайтл из нескольких совпадений каталога yani на один внешний ID
 * (например, оригинальный сериал и режиссёрская версия с общим `shikimori_id`).
 * Если один из кандидатов уже лежит в библиотеке пользователя, предпочитаем его.
 */
fun List<ResolvedExternalAnime>.selectPrimaryCandidate(
    existingAnimeIds: Set<Int>,
): ResolvedExternalAnime? {
    if (isEmpty()) return null
    if (size == 1) return first()
    firstOrNull { it.animeId in existingAnimeIds }?.let { return it }
    return sortedWith(
        compareBy<ResolvedExternalAnime> { it.title.isSecondaryEditionTitle() }
            .thenBy { it.animeId },
    ).first()
}

/** Выбирает внешний ID тайтла под целевой формат экспорта с запасным переходом на парный ID. */
fun ExportableLibraryAnime.externalIdFor(format: ExternalListFormat): Int? {
    val shiki = shikimoriId?.takeIf { it > 0 }
    val mal = myAnimeListId?.takeIf { it > 0 }
    return when (format) {
        ExternalListFormat.SHIKIMORI_JSON -> shiki ?: mal
        ExternalListFormat.MAL_XML -> mal ?: shiki
    }
}

/**
 * Подбирает экспортируемое число просмотренных серий без сетевых запросов к `/anime/{id}/videos`:
 * для «В планах» — всегда `0`; для полнометражки в «Просмотрено» без локальных отметок — `1`;
 * иначе — число локально досмотренных серий (`0` для «Просмотрено» валидно принимают импортёры
 * Shikimori и MyAnimeList, подставляя общее число серий из своей базы).
 */
fun resolveExportedWatchedEpisodes(
    list: UserAnimeList,
    typeAlias: String?,
    localWatchedCount: Int,
): Int =
    when (list) {
        UserAnimeList.PLANNED -> 0
        UserAnimeList.COMPLETED -> when {
            localWatchedCount > 0 -> localWatchedCount
            typeAlias.equals("movie", ignoreCase = true) -> 1
            else -> 0
        }
        UserAnimeList.WATCHING,
        UserAnimeList.POSTPONED,
        UserAnimeList.DROPPED,
        -> localWatchedCount.coerceAtLeast(0)
    }

/** Строит имя файла экспорта по формату и дате. */
fun buildExportFileName(format: ExternalListFormat, nowMillis: Long): String {
    val date = EXPORT_DATE_FORMATTER.format(Instant.ofEpochMilli(nowMillis))
    return when (format) {
        ExternalListFormat.MAL_XML -> "yummytv-animelist-$date.${format.fileExtension}"
        ExternalListFormat.SHIKIMORI_JSON -> "yummytv-shikimori-animes-$date.${format.fileExtension}"
    }
}

/** Вычисляет паузу перед повтором запроса (`attempt` с нуля), учитывая `Retry-After` и HTTP 429. */
fun Throwable.computeImportRetryDelayMs(attempt: Int): Long {
    retryAfterMillisOrNull()?.let { return it }
    val base = if (isTooManyRequests()) RATE_LIMIT_BASE_DELAY_MS else BASE_RETRY_DELAY_MS
    val factor = 1L shl attempt.coerceIn(0, 4)
    return (base * factor).coerceAtMost(MAX_BACKOFF_DELAY_MS)
}

/** Нужно ли помечать серии локально для данного статуса при включённой опции импорта серий. */
fun UserAnimeList.supportsLocalEpisodeImport(): Boolean =
    when (this) {
        UserAnimeList.WATCHING,
        UserAnimeList.POSTPONED,
        UserAnimeList.DROPPED,
        -> true
        UserAnimeList.PLANNED,
        UserAnimeList.COMPLETED,
        -> false
    }

private fun String.isSecondaryEditionTitle(): Boolean {
    val lower = lowercase(Locale.ROOT)
    return SECONDARY_EDITION_MARKERS.any { marker -> lower.contains(marker) }
}
