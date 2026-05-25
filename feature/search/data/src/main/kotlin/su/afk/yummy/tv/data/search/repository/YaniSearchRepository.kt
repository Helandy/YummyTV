package su.afk.yummy.tv.data.search.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.search.mapper.toSearchAnimeType
import su.afk.yummy.tv.data.search.mapper.toSearchGenre
import su.afk.yummy.tv.data.search.mapper.toSearchGenreGroup
import su.afk.yummy.tv.data.search.mapper.toSearchItem
import su.afk.yummy.tv.data.search.network.YaniSearchApi
import su.afk.yummy.tv.domain.search.SearchFilterOptions
import su.afk.yummy.tv.domain.search.SearchFilters
import su.afk.yummy.tv.domain.search.SearchPage
import su.afk.yummy.tv.domain.search.SearchRepository

class YaniSearchRepository(
    private val api: YaniSearchApi,
) : SearchRepository {
    override suspend fun search(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): SearchPage = withContext(Dispatchers.IO) {
        val response = api.search(query, filters, limit, offset)
        SearchPage(
            items = response.mapNotNull { it.toSearchItem() },
            nextOffset = offset + response.size,
            canLoadMore = response.size >= limit,
        )
    }

    override suspend fun getFilterOptions(): SearchFilterOptions = withContext(Dispatchers.IO) {
        coroutineScope {
            val genres = async { api.getGenres() }
            val catalog = async { api.getCatalog() }
            SearchFilterOptions(
                genreGroups = genres.await().groups.mapNotNull { it.toSearchGenreGroup() },
                genres = genres.await().genres.mapNotNull { it.toSearchGenre() },
                types = catalog.await().types.mapNotNull { it.toSearchAnimeType() },
            )
        }
    }
}
