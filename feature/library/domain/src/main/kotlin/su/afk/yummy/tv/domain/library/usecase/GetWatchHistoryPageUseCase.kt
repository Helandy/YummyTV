package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.repository.WatchHistoryRepository
import javax.inject.Inject

/** Загружает страницу истории просмотров текущего пользователя. */
class GetWatchHistoryPageUseCase @Inject constructor(
    private val repository: WatchHistoryRepository,
) {
    suspend operator fun invoke(limit: Int, offset: Int) = repository.getPage(limit, offset)
}
