package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

class SetAnimeRecommendationIgnoredUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int, ignored: Boolean): Boolean =
        repository.setAnimeRecommendationIgnored(animeId, ignored)
}
