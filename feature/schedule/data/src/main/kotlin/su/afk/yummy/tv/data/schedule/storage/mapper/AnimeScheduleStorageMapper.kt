package su.afk.yummy.tv.data.schedule.storage.mapper

import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleCache
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleCacheEntry
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleItemEntry
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.schedule.dto.YaniScheduleAnimeDto
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

internal fun List<YaniScheduleAnimeDto>.toAnimeScheduleCache(
    language: String,
    cachedAt: Long,
): AnimeScheduleCache =
    AnimeScheduleCache(
        entry = AnimeScheduleCacheEntry(
            language = language,
            cachedAt = cachedAt,
        ),
        items = mapNotNull { item ->
            val animeId = item.animeId ?: return@mapNotNull null
            animeId to item
        }.mapIndexed { index, (animeId, item) ->
            AnimeScheduleItemEntry(
                language = language,
                position = index,
                animeId = animeId,
                title = item.title,
                posterUrl = item.poster?.run { mega ?: big ?: medium ?: fullsize ?: small }
                    ?.toHttpsUrl(),
                nextDateEpochSeconds = item.episodes?.nextDate?.takeIf { it > 0 },
                previousDateEpochSeconds = item.episodes?.prevDate?.takeIf { it > 0 },
                airedEpisodes = item.episodes?.aired?.takeIf { it > 0 },
                totalEpisodes = item.episodes?.count?.takeIf { it > 0 },
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
