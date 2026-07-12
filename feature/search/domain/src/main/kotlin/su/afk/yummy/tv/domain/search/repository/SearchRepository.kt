package su.afk.yummy.tv.domain.search.repository

import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchPage

interface SearchRepository {
    suspend fun search(query: String, filters: SearchFilters, limit: Int, offset: Int): SearchPage
    suspend fun getRandomAnime(): SearchItem?
    suspend fun getFilterOptions(): SearchFilterOptions
}
