package su.afk.yummy.tv.domain.search

class SearchUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(
        query: String,
        filters: SearchFilters = SearchFilters.EMPTY,
        limit: Int = 40,
        offset: Int = 0,
    ): SearchPage = repository.search(query, filters, limit, offset)
}

class GetSearchFilterOptionsUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(): SearchFilterOptions = repository.getFilterOptions()
}
