package su.afk.yummy.tv.data.search

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.domain.search.SearchAnimeType
import su.afk.yummy.tv.domain.search.SearchFilterOptions
import su.afk.yummy.tv.domain.search.SearchFilters
import su.afk.yummy.tv.domain.search.SearchGenre
import su.afk.yummy.tv.domain.search.SearchGenreGroup
import su.afk.yummy.tv.domain.search.SearchItem
import su.afk.yummy.tv.domain.search.SearchRepository

class YaniSearchRepository(
    private val client: HttpClient,
) : SearchRepository {
    override suspend fun search(
        query: String,
        filters: SearchFilters,
        limit: Int,
        offset: Int,
    ): List<SearchItem> = withContext(Dispatchers.IO) {
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
        }
            .body<YaniSearchResponseDto>()
            .response
            .mapNotNull { it.toSearchItem() }
    }

    override suspend fun getFilterOptions(): SearchFilterOptions = withContext(Dispatchers.IO) {
        coroutineScope {
            val genres = async {
                client.get("$YANI_BASE_URL/anime/genres")
                    .body<YaniSearchGenresResponseDto>()
                    .response
            }
            val catalog = async {
                client.get("$YANI_BASE_URL/anime/catalog") {
                    parameter("limit", 1)
                    parameter("offset", 0)
                }
                    .body<YaniSearchCatalogResponseDto>()
                    .response
            }
            SearchFilterOptions(
                genreGroups = genres.await().groups.mapNotNull { it.toSearchGenreGroup() },
                genres = genres.await().genres.mapNotNull { it.toSearchGenre() },
                types = catalog.await().types.mapNotNull { it.toSearchAnimeType() },
            )
        }
    }
}

private fun YaniSearchItemDto.toSearchItem(): SearchItem? {
    val id = animeId ?: return null
    return SearchItem(
        id = id,
        title = title,
        posterUrl = poster?.run { medium ?: big ?: fullsize ?: small }?.toHttpsUrl(),
        rating = rating?.average,
    )
}

private fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}

private fun YaniSearchGenreGroupDto.toSearchGenreGroup(): SearchGenreGroup? {
    val id = id ?: return null
    return SearchGenreGroup(id = id, title = title)
}

private fun YaniSearchGenreDto.toSearchGenre(): SearchGenre? {
    val id = value ?: return null
    return SearchGenre(
        id = id.toString(),
        title = title,
        groupId = groupId ?: 0,
    )
}

private fun YaniSearchTypeCountDto.toSearchAnimeType(): SearchAnimeType? {
    val type = type ?: return null
    val id = type.alias ?: type.value?.toString() ?: return null
    return SearchAnimeType(id = id, title = type.name)
}
