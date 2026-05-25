package su.afk.yummy.tv.domain.schedule

interface AnimeScheduleRepository {
    suspend fun getSchedule(): List<AnimeScheduleDay>
}
