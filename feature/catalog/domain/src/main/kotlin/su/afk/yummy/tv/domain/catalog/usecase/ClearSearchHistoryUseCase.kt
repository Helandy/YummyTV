package su.afk.yummy.tv.domain.catalog.usecase

import su.afk.yummy.tv.domain.catalog.repository.SearchHistoryRepository
import javax.inject.Inject

/** Очищает историю поисковых запросов каталога. */
class ClearSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
