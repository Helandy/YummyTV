package su.afk.yummy.tv.domain.schedule

data class AnimeScheduleDay(
    val title: String,
    val items: List<AnimeScheduleItem>,
)

data class AnimeScheduleItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val nextDateEpochSeconds: Long?,
    val airedEpisodes: Int?,
    val totalEpisodes: Int?,
    val previousDateEpochSeconds: Long? = null,
)

interface AnimeScheduleRepository {
    suspend fun getSchedule(): List<AnimeScheduleDay>
}

class GetAnimeScheduleUseCase(private val repository: AnimeScheduleRepository) {
    suspend operator fun invoke(): List<AnimeScheduleDay> = repository.getSchedule()
}
