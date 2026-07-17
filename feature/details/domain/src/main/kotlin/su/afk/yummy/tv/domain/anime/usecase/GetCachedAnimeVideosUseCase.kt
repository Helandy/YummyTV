package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/** Читает кешированные видео аниме без сетевого обновления. */
class GetCachedAnimeVideosUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(animeId: Int): List<AnimeVideo>? =
        repository.getCachedAnimeVideos(animeId)
}
