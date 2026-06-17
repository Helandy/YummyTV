package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

class ObserveAnimeWatchProgressUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    operator fun invoke(animeId: Int) = repository.observeWatchProgress(animeId)
}
