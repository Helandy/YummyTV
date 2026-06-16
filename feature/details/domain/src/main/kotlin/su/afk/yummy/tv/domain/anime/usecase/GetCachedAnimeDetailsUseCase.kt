package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/** Loads cached anime details without refreshing from the network. */
class GetCachedAnimeDetailsUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): AnimeDetails? =
        repository.getCachedAnimeDetails(animeId)
}
