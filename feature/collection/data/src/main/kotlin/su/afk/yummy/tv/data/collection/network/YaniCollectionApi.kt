package su.afk.yummy.tv.data.collection.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.collection.dto.YaniCollectionDetailResponseDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionListResponseDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionVoteBodyDto
import su.afk.yummy.tv.data.collection.dto.YaniCollectionVoteResponseDto

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

    suspend fun voteCollection(
        id: Int,
        body: YaniCollectionVoteBodyDto,
    ): YaniCollectionVoteResponseDto =
        client.put("$YANI_BASE_URL/collection/$id/vote") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun removeCollectionVote(id: Int): YaniCollectionVoteResponseDto =
        client.delete("$YANI_BASE_URL/collection/$id/vote").body()
}
