package su.afk.yummy.tv.core.storage.schedule

class AnimeScheduleStore(private val dao: AnimeScheduleDao) {

    suspend fun getSchedule(language: String): AnimeScheduleCache? =
        dao.getSchedule(language)

    suspend fun saveSchedule(cache: AnimeScheduleCache) {
        dao.replaceSchedule(cache)
    }
}
