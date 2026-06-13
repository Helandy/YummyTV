package su.afk.yummy.tv.domain.search.usecase

import su.afk.yummy.tv.domain.search.model.*
import su.afk.yummy.tv.domain.search.repository.*
import javax.inject.Inject

/** Searches anime with the selected query, filters, and paging. */
class SearchUseCase @Inject constructor(private val repository: SearchRepository) {
    suspend operator fun invoke(
        query: String,
        filters: SearchFilters = SearchFilters.EMPTY,
        limit: Int = 40,
        offset: Int = 0,
    ): SearchPage = repository.search(query, filters, limit, offset)
}
