package su.afk.yummy.tv.domain.schedule.usecase

import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository

/** Loads the weekly anime release schedule. */
class GetAnimeScheduleUseCase(private val repository: AnimeScheduleRepository) {
    suspend operator fun invoke(): List<AnimeScheduleDay> = repository.getSchedule()
}
