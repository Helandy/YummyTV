package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository

/** Loads playable video entries for an anime. */
class GetAnimeVideosUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeVideo> = repository.getAnimeVideos(animeId)
}
