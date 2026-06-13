package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/** Loads playable video entries for an anime. */
class GetAnimeVideosUseCase @Inject constructor(private val repository: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeVideo> = repository.getAnimeVideos(animeId)
}
