package su.afk.yummy.tv.data.schedule

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.domain.schedule.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.AnimeScheduleItem
import su.afk.yummy.tv.domain.schedule.AnimeScheduleRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private const val SCHEDULE_TTL_MS = 60 * 60 * 1000L

class YaniScheduleRepository(
    private val client: HttpClient,
    private val cache: CacheStore,
    private val json: Json,
) : AnimeScheduleRepository {

    override suspend fun getSchedule(): List<AnimeScheduleDay> =
        cache.getOrFetch(
            key = "anime_schedule",
            ttlMs = SCHEDULE_TTL_MS,
            serialize = { dto: YaniScheduleResponseDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { client.get("$YANI_BASE_URL/anime/schedule").body<YaniScheduleResponseDto>() },
        ).response
            .mapNotNull { it.toScheduleItem() }
            .groupBy { it.nextDateEpochSeconds?.toDayTitle().orEmpty().ifBlank { "Schedule" } }
            .map { (title, items) -> AnimeScheduleDay(title, items.sortedBy { it.nextDateEpochSeconds ?: Long.MAX_VALUE }) }
}

private fun YaniScheduleAnimeDto.toScheduleItem(): AnimeScheduleItem? {
    val id = animeId ?: return null
    return AnimeScheduleItem(
        animeId = id,
        title = title,
        posterUrl = poster?.mega?.toHttpsUrl() ?: poster?.big?.toHttpsUrl() ?: poster?.medium?.toHttpsUrl() ?: poster?.fullsize?.toHttpsUrl() ?: poster?.small?.toHttpsUrl(),
        nextDateEpochSeconds = episodes?.nextDate?.takeIf { it > 0 },
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
