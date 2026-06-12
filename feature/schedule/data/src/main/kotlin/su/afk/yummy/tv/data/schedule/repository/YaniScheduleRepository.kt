package su.afk.yummy.tv.data.schedule.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.schedule.dto.YaniScheduleResponseDto
import su.afk.yummy.tv.data.schedule.mapper.toScheduleDays
import su.afk.yummy.tv.data.schedule.network.YaniScheduleApi
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository

private const val SCHEDULE_TTL_MS = 60 * 60 * 1000L

class YaniScheduleRepository(
    private val api: YaniScheduleApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : AnimeScheduleRepository {

    override suspend fun getSchedule(): List<AnimeScheduleDay> = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        cache.getOrFetch(
            key = "anime_schedule_v2".withYaniContentLanguage(language),
            ttlMs = SCHEDULE_TTL_MS,
            serialize = { dto: YaniScheduleResponseDto -> json.encodeToString(dto) },
            deserialize = { json.decodeFromString(it) },
            fetch = { api.getSchedule() },
        ).response.toScheduleDays()
    }
}
