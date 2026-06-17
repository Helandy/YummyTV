package su.afk.yummy.tv.data.collection.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailResponseDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionListResponseDto

class YaniCollectionApi(
    private val client: HttpClient,
) {
    suspend fun getCollection(id: Int): YaniCollectionDetailResponseDto =
        client.get("$YANI_BASE_URL/collection/$id").body()

    suspend fun getCollections(limit: Int, offset: Int): YaniCollectionListResponseDto =
        client.get("$YANI_BASE_URL/collection") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
}
