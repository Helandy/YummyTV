package su.afk.yummy.tv.data.posts.network

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
import su.afk.yummy.tv.data.posts.dto.YaniPostCategoriesResponseDto
import su.afk.yummy.tv.data.posts.dto.YaniPostDetailsResponseDto
import su.afk.yummy.tv.data.posts.dto.YaniPostVoteBodyDto
import su.afk.yummy.tv.data.posts.dto.YaniPostVoteResponseDto
import su.afk.yummy.tv.data.posts.dto.YaniPostsResponseDto
import javax.inject.Inject

class YaniPostsApi @Inject constructor(private val clientProvider: YaniHttpClientProvider) {
    suspend fun categories(): YaniPostCategoriesResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/posts/categories").body()

    suspend fun posts(
        category: String?,
        sort: String,
        limit: Int,
        skip: Int
    ): YaniPostsResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/posts") {
            category?.let { parameter("category", it) }
            parameter("sort", sort); parameter("limit", limit); parameter("skip", skip)
        }.body()

    suspend fun details(postId: Int): YaniPostDetailsResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/posts/$postId").body()

    suspend fun vote(postId: Int, action: Int): YaniPostVoteResponseDto =
        clientProvider.get().put("$YANI_BASE_URL/posts/$postId/vote") {
            contentType(ContentType.Application.Json); setBody(YaniPostVoteBodyDto(action))
        }.body()

    suspend fun removeVote(postId: Int): YaniPostVoteResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/posts/$postId/vote").body()
}
