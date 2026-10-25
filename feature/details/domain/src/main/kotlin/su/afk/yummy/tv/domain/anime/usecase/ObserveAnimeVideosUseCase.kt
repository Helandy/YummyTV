package su.afk.yummy.tv.domain.anime.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/**
 * Поэтапно отдаёт видео выбранного аниме: сначала основную выдачу, не дожидаясь модулей-источников,
 * затем её же вместе с их озвучками, когда те ответили.
 */
class ObserveAnimeVideosUseCase @Inject constructor(private val repository: AnimeRepository) {
    operator fun invoke(animeId: Int): Flow<List<AnimeVideo>> = repository.observeAnimeVideos(animeId)
}
