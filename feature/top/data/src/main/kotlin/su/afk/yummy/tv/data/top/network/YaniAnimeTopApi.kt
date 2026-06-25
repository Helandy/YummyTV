package su.afk.yummy.tv.data.top.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.top.dto.YaniAnimeTopListDto
import su.afk.yummy.tv.domain.top.model.AnimeTopType

class YaniAnimeTopApi(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): YaniAnimeTopListDto =
        clientProvider.get().get("$YANI_BASE_URL/anime") {
            parameter("sort", "top")
            parameter("types", type.apiValue)
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("sort_forward", true)
            parameter("from_year", 1900)
        }.body()
}
