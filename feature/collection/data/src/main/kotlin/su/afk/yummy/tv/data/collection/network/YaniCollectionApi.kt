package su.afk.yummy.tv.data.collection.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailResponseDto

class YaniCollectionApi(
    private val client: HttpClient,
) {
    suspend fun getCollection(id: Int): YaniCollectionDetailResponseDto =
        client.get("$YANI_BASE_URL/collection/$id").body()
}
