package su.afk.yummy.tv.data.schedule.mapper

import su.afk.yummy.tv.data.schedule.dto.YaniScheduleAnimeDto
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

internal fun List<YaniScheduleAnimeDto>.toScheduleDays(): List<AnimeScheduleDay> =
    mapNotNull { it.toScheduleItem() }
        .groupBy { it.nextDateEpochSeconds?.toDayTitle().orEmpty().ifBlank { "Schedule" } }
        .map { (title, items) -> AnimeScheduleDay(title, items.sortedBy { it.nextDateEpochSeconds ?: Long.MAX_VALUE }) }

private fun YaniScheduleAnimeDto.toScheduleItem(): AnimeScheduleItem? {
    val id = animeId ?: return null
    return AnimeScheduleItem(
        animeId = id,
        title = title,
        posterUrl = poster?.mega?.toHttpsUrl() ?: poster?.big?.toHttpsUrl() ?: poster?.medium?.toHttpsUrl() ?: poster?.fullsize?.toHttpsUrl() ?: poster?.small?.toHttpsUrl(),
        nextDateEpochSeconds = episodes?.nextDate?.takeIf { it > 0 },
        previousDateEpochSeconds = episodes?.prevDate?.takeIf { it > 0 },
        airedEpisodes = episodes?.aired?.takeIf { it > 0 },
        totalEpisodes = episodes?.count?.takeIf { it > 0 },
    )
}

private fun Long.toDayTitle(): String =
    Instant.ofEpochSecond(this)
        .atZone(ZoneId.systemDefault())
        .dayOfWeek
        .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())

private fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}
