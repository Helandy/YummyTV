package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Возвращает известные идентификаторы видео для продолжения просмотра тайтла. */
class GetContinueWatchingVideoIdsUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(animeId: Int): List<Int> =
        homeFeedRepository.getContinueWatchingVideoIds(animeId)
}
