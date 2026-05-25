package su.afk.yummy.tv.domain.schedule

/** Loads the weekly anime release schedule. */
class GetAnimeScheduleUseCase(private val repository: AnimeScheduleRepository) {
    suspend operator fun invoke(): List<AnimeScheduleDay> = repository.getSchedule()
}
