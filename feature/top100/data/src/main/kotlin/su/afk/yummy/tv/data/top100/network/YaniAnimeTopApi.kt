package su.afk.yummy.tv.data.top100.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.top100.dto.YaniAnimeTopListDto
import su.afk.yummy.tv.domain.top100.model.AnimeTopType

class YaniAnimeTopApi(
    private val client: HttpClient,
) {
    suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): YaniAnimeTopListDto =
        client.get("$YANI_BASE_URL/anime") {
            parameter("sort", "top")
            parameter("types", type.apiValue)
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("sort_forward", true)
            parameter("from_year", 1900)
        }.body()
}
