package su.afk.yummy.tv.domain.search.repository

import su.afk.yummy.tv.domain.search.model.*

interface SearchRepository {
    suspend fun search(query: String, filters: SearchFilters, limit: Int, offset: Int): SearchPage
    suspend fun getFilterOptions(): SearchFilterOptions
}
