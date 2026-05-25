package su.afk.yummy.tv.domain.top100

class GetAnimeTopUseCase(
    private val repository: AnimeTopRepository,
) {
    suspend operator fun invoke(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        repository.getTopAnime(type, limit, offset)
}
