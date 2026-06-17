package su.afk.yummy.tv.domain.search.usecase

import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchPage
import su.afk.yummy.tv.domain.search.repository.SearchRepository
import javax.inject.Inject

/** Ищет аниме по запросу, фильтрам и параметрам страницы. */
class SearchUseCase @Inject constructor(private val repository: SearchRepository) {
    suspend operator fun invoke(
        query: String,
        filters: SearchFilters = SearchFilters.EMPTY,
        limit: Int = 40,
        offset: Int = 0,
    ): SearchPage = repository.search(query, filters, limit, offset)
}
