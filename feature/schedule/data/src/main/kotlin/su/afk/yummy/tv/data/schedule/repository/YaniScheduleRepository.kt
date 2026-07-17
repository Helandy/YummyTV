package su.afk.yummy.tv.data.schedule.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleStore
import su.afk.yummy.tv.core.storage.schedule.isFresh
import su.afk.yummy.tv.data.schedule.network.YaniScheduleApi
import su.afk.yummy.tv.data.schedule.storage.mapper.toAnimeScheduleCache
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository
import su.afk.yummy.tv.data.schedule.storage.mapper.toScheduleDays as toStoredScheduleDays

private const val SCHEDULE_TTL_MS = 60 * 60 * 1000L

class YaniScheduleRepository(
    private val api: YaniScheduleApi,
    private val scheduleStore: AnimeScheduleStore,
    private val settingsStore: SettingsStore,
) : AnimeScheduleRepository {

    override suspend fun getSchedule(): List<AnimeScheduleDay> = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = scheduleStore.getSchedule(languageCode)
        if (stored?.isFresh(SCHEDULE_TTL_MS) == true) {
            return@withContext stored.toStoredScheduleDays()
        }

        try {
            fetchSchedule(languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toStoredScheduleDays()
                ?: throw error
        }
    }

    private suspend fun fetchSchedule(languageCode: String): List<AnimeScheduleDay> {
        val cache = api.getSchedule().response.toAnimeScheduleCache(
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        scheduleStore.saveSchedule(cache)
        return cache.toStoredScheduleDays()
    }
}
