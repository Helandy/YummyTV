package su.afk.yummy.tv.domain.search

interface SearchRepository {
    suspend fun search(query: String, filters: SearchFilters, limit: Int, offset: Int): List<SearchItem>
    suspend fun getFilterOptions(): SearchFilterOptions
}
