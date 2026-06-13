package su.afk.yummy.tv.data.schedule.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.settings.withYaniContentLanguage
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleStore
import su.afk.yummy.tv.core.storage.schedule.isFresh
import su.afk.yummy.tv.data.schedule.dto.YaniScheduleResponseDto
import su.afk.yummy.tv.data.schedule.mapper.toAnimeScheduleCache
import su.afk.yummy.tv.data.schedule.mapper.toScheduleDays
import su.afk.yummy.tv.data.schedule.network.YaniScheduleApi
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository

private const val SCHEDULE_TTL_MS = 60 * 60 * 1000L

class YaniScheduleRepository(
    private val api: YaniScheduleApi,
    private val cache: CacheStore,
    private val scheduleStore: AnimeScheduleStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : AnimeScheduleRepository {

    override suspend fun getSchedule(): List<AnimeScheduleDay> = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = scheduleStore.getSchedule(languageCode)
        if (stored?.isFresh(SCHEDULE_TTL_MS) == true) {
            return@withContext stored.toScheduleDays()
        }

        try {
            fetchSchedule(languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toScheduleDays()
                ?: readLegacySchedule(language, languageCode)
                ?: throw error
        }
    }

    private suspend fun fetchSchedule(languageCode: String): List<AnimeScheduleDay> {
        val days = api.getSchedule().response.toScheduleDays()
        scheduleStore.saveSchedule(
            days.toAnimeScheduleCache(
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return days
    }

    private suspend fun readLegacySchedule(
        language: YaniContentLanguage,
        languageCode: String,
    ): List<AnimeScheduleDay>? {
        val cached = cache.getCached<YaniScheduleResponseDto>(
            key = scheduleCacheKey(language),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val days = cached.value.response.toScheduleDays()
        scheduleStore.saveSchedule(
            days.toAnimeScheduleCache(
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return days
    }

    private fun scheduleCacheKey(language: YaniContentLanguage): String =
        "anime_schedule_v2".withYaniContentLanguage(language)
}
