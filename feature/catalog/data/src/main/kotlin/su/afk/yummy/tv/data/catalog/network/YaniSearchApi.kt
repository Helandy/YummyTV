package su.afk.yummy.tv.data.catalog.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.yani.YANI_BASE_URL
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.data.catalog.dto.YaniSearchGenresDto
import su.afk.yummy.tv.data.catalog.dto.YaniSearchGenresResponseDto
import su.afk.yummy.tv.data.catalog.dto.YaniSearchItemDto
import su.afk.yummy.tv.data.catalog.dto.YaniSearchResponseDto
import su.afk.yummy.tv.domain.catalog.model.SearchFilters

class YaniSearchApi(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun search(query: String, filters: SearchFilters, limit: Int, offset: Int): List<YaniSearchItemDto> =
        clientProvider.get().get("$YANI_BASE_URL/anime") {
            query.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
            filters.genres.forEach { parameter("genres", it) }
            filters.excludedGenres.forEach { parameter("exclude_genres", it) }
            filters.types.forEach { parameter("types", it) }
            filters.statuses.forEach { parameter("status", it) }
            filters.fromYear?.let { parameter("from_year", it) }
            filters.toYear?.let { parameter("to_year", it) }
            filters.seasons.forEach { parameter("season", it) }
            filters.ageRatings.forEach { parameter("min_age", it) }
            parameter("sort", filters.sort.apiValue)
            parameter("sort_forward", filters.sortForward)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniSearchResponseDto>().response

    suspend fun getRandomAnime(): List<YaniSearchItemDto> =
        clientProvider.get().get("$YANI_BASE_URL/anime") {
            parameter("sort", "random")
            parameter("limit", 1)
            parameter("offset", 0)
        }.body<YaniSearchResponseDto>().response

    suspend fun getGenres(): YaniSearchGenresDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/genres")
            .body<YaniSearchGenresResponseDto>()
            .response

}
