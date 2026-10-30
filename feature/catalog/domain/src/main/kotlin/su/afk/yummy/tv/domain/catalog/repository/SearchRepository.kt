package su.afk.yummy.tv.domain.catalog.repository

import su.afk.yummy.tv.domain.catalog.model.CachedSearchPage
import su.afk.yummy.tv.domain.catalog.model.SearchFilterOptions
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.model.SearchPage

interface SearchRepository {
    suspend fun search(query: String, filters: SearchFilters, limit: Int, offset: Int): SearchPage
    suspend fun getCachedSearchPage(query: String, filters: SearchFilters, limit: Int, offset: Int): CachedSearchPage?
    suspend fun getRandomAnime(): SearchItem?
    suspend fun getFilterOptions(): SearchFilterOptions
}
