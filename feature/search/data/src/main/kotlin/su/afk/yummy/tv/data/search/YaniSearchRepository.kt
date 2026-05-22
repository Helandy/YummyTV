package su.afk.yummy.tv.data.search

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.domain.search.SearchItem
import su.afk.yummy.tv.domain.search.SearchRepository

class YaniSearchRepository(
    private val client: HttpClient,
) : SearchRepository {
    override suspend fun search(query: String, limit: Int, offset: Int): List<SearchItem> = withContext(Dispatchers.IO) {
        client.get("$YANI_BASE_URL/search") {
            parameter("q", query)
            parameter("limit", limit)
            parameter("offset", offset)
        }
            .body<YaniSearchResponseDto>()
            .response
            .mapNotNull { it.toSearchItem() }
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
