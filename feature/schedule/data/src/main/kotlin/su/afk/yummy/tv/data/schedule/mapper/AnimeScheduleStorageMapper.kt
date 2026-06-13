package su.afk.yummy.tv.data.schedule.mapper

import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleCache
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleCacheEntry
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleItemEntry
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

internal fun List<AnimeScheduleDay>.toAnimeScheduleCache(
    language: String,
    cachedAt: Long,
): AnimeScheduleCache =
    AnimeScheduleCache(
        entry = AnimeScheduleCacheEntry(
            language = language,
            cachedAt = cachedAt,
        ),
        items = flatMap { it.items }
            .mapIndexed { index, item ->
                AnimeScheduleItemEntry(
                    language = language,
                    position = index,
                    animeId = item.animeId,
                    title = item.title,
                    posterUrl = item.posterUrl,
                    nextDateEpochSeconds = item.nextDateEpochSeconds,
                    previousDateEpochSeconds = item.previousDateEpochSeconds,
                    airedEpisodes = item.airedEpisodes,
                    totalEpisodes = item.totalEpisodes,
                )
            },
    )

internal fun AnimeScheduleCache.toScheduleDays(): List<AnimeScheduleDay> =
    items
        .sortedBy { it.position }
        .map {
            AnimeScheduleItem(
                animeId = it.animeId,
                title = it.title,
                posterUrl = it.posterUrl,
                nextDateEpochSeconds = it.nextDateEpochSeconds,
                previousDateEpochSeconds = it.previousDateEpochSeconds,
                airedEpisodes = it.airedEpisodes,
                totalEpisodes = it.totalEpisodes,
            )
        }
        .groupBy { it.nextDateEpochSeconds?.toDayTitle().orEmpty().ifBlank { "Schedule" } }
        .map { (title, items) ->
            AnimeScheduleDay(
                title = title,
                items = items.sortedBy { it.nextDateEpochSeconds ?: Long.MAX_VALUE },
            )
        }

private fun Long.toDayTitle(): String =
    Instant.ofEpochSecond(this)
        .atZone(ZoneId.systemDefault())
        .dayOfWeek
        .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
