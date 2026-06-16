package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/** Reloads playable video entries from the network, bypassing the Room video cache. */
class RefreshAnimeVideosUseCase @Inject constructor(private val repository: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeVideo> =
        repository.refreshAnimeVideos(animeId)
}
