package su.afk.yummy.tv.domain.schedule.usecase

import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository
import javax.inject.Inject

/** Loads the weekly anime release schedule. */
class GetAnimeScheduleUseCase @Inject constructor(private val repository: AnimeScheduleRepository) {
    suspend operator fun invoke(): List<AnimeScheduleDay> = repository.getSchedule()
}
