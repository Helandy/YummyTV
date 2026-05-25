package su.afk.yummy.tv.data.search.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.search.dto.YaniSearchCatalogDto
import su.afk.yummy.tv.data.search.dto.YaniSearchCatalogResponseDto
import su.afk.yummy.tv.data.search.dto.YaniSearchGenresDto
import su.afk.yummy.tv.data.search.dto.YaniSearchGenresResponseDto
import su.afk.yummy.tv.data.search.dto.YaniSearchItemDto
import su.afk.yummy.tv.data.search.dto.YaniSearchResponseDto
import su.afk.yummy.tv.domain.search.model.SearchFilters

class YaniSearchApi(
    private val client: HttpClient,
) {
    suspend fun search(query: String, filters: SearchFilters, limit: Int, offset: Int): List<YaniSearchItemDto> =
        client.get("$YANI_BASE_URL/anime") {
            query.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
            filters.genres.forEach { parameter("genres", it) }
            filters.excludedGenres.forEach { parameter("exclude_genres", it) }
            filters.types.forEach { parameter("types", it) }
            filters.statuses.forEach { parameter("status", it) }
            filters.fromYear?.let { parameter("from_year", it) }
            filters.toYear?.let { parameter("to_year", it) }
            filters.seasons.forEach { parameter("season", it) }
            filters.ageRatings.forEach { parameter("min_age", it) }
            filters.sort.apiValue?.let { parameter("sort", it) }
            parameter("sort_forward", filters.sortForward)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniSearchResponseDto>().response

    suspend fun getGenres(): YaniSearchGenresDto =
        client.get("$YANI_BASE_URL/anime/genres")
            .body<YaniSearchGenresResponseDto>()
            .response

    suspend fun getCatalog(): YaniSearchCatalogDto =
        client.get("$YANI_BASE_URL/anime/catalog") {
            parameter("limit", 1)
            parameter("offset", 0)
        }.body<YaniSearchCatalogResponseDto>().response
}
