package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.core.model.anime.AnimeRecommendationReaction
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

class VoteAnimeRecommendationUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(
        animeId: Int,
        similarAnimeId: Int,
        vote: AnimeRecommendationVote,
    ): AnimeRecommendationReaction =
        repository.voteAnimeRecommendation(animeId, similarAnimeId, vote)
}
