package su.afk.yummy.tv.data.collection.network

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailResponseDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionListResponseDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionVoteBodyDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionVoteResponseDto

class YaniCollectionApi(
    private val clientProvider: YaniHttpClientProvider,
) {
    suspend fun getCollection(id: Int): YaniCollectionDetailResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/collection/$id").body()

    suspend fun getCollections(limit: Int, offset: Int): YaniCollectionListResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/collection") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun voteCollection(
        id: Int,
        body: YaniCollectionVoteBodyDto,
    ): YaniCollectionVoteResponseDto =
        clientProvider.get().put("$YANI_BASE_URL/collection/$id/vote") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun removeCollectionVote(id: Int): YaniCollectionVoteResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/collection/$id/vote").body()
}
