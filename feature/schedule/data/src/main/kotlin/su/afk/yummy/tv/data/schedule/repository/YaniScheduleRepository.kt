package su.afk.yummy.tv.data.schedule.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleStorage
import su.afk.yummy.tv.core.storage.schedule.isFresh
import su.afk.yummy.tv.data.schedule.network.YaniScheduleApi
import su.afk.yummy.tv.data.schedule.storage.mapper.toAnimeScheduleCache
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository
import su.afk.yummy.tv.data.schedule.storage.mapper.toScheduleDays as toStoredScheduleDays

private const val SCHEDULE_TTL_MS = 60 * 60 * 1000L

class YaniScheduleRepository(
    private val api: YaniScheduleApi,
    private val scheduleStore: AnimeScheduleStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : AnimeScheduleRepository {

    override suspend fun getSchedule(): List<AnimeScheduleDay> = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.currentLanguageCode()
        offlineFirstCache(
            read = { scheduleStore.getSchedule(languageCode) },
            isFresh = { it.isFresh(SCHEDULE_TTL_MS) },
            toDomain = { it.toStoredScheduleDays() },
            fetchAndSave = {
                val cache = api.getSchedule().response.toAnimeScheduleCache(
                    language = languageCode,
                    cachedAt = System.currentTimeMillis(),
                )
                scheduleStore.saveSchedule(cache)
                cache
            },
        )
    }
}
