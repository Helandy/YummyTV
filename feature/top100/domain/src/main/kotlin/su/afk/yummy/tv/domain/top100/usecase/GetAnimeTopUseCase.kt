package su.afk.yummy.tv.domain.top100

/** Loads a paged top anime list for the selected ranking type. */
class GetAnimeTopUseCase(
    private val repository: AnimeTopRepository,
) {
    suspend operator fun invoke(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        repository.getTopAnime(type, limit, offset)
}
