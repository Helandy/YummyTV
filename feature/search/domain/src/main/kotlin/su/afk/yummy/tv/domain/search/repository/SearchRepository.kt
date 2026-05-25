package su.afk.yummy.tv.domain.search

interface SearchRepository {
    suspend fun search(query: String, filters: SearchFilters, limit: Int, offset: Int): SearchPage
    suspend fun getFilterOptions(): SearchFilterOptions
}
