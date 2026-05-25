package su.afk.yummy.tv.domain.schedule.repository

import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay

interface AnimeScheduleRepository {
    suspend fun getSchedule(): List<AnimeScheduleDay>
}
