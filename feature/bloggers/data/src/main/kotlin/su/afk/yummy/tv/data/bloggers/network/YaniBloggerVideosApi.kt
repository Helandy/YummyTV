package su.afk.yummy.tv.data.bloggers.network

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
import su.afk.yummy.tv.data.bloggers.dto.BloggerDetailsResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerSubscriptionResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoReactionResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideoVoteBodyDto
import su.afk.yummy.tv.data.bloggers.dto.BloggerVideosResponseDto
import su.afk.yummy.tv.data.bloggers.dto.BloggersResponseDto
import javax.inject.Inject

class YaniBloggerVideosApi @Inject constructor(private val clientProvider: YaniHttpClientProvider) {
    suspend fun getVideos(
        category: String,
        bloggerId: Int?,
        sort: String,
        limit: Int,
        offset: Int
    ): BloggerVideosResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/bloggers/video") {
            parameter("category", category)
            bloggerId?.let { parameter("blogger_id", it) }
            parameter("sort", sort)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun getAnimeVideos(animeId: Int, limit: Int, offset: Int): BloggerVideosResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/bloggervideos") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun getDirectory(limit: Int): BloggersResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/bloggers") { parameter("limit", limit) }.body()

    suspend fun getBlogger(id: Int): BloggerDetailsResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/bloggers/$id").body()

    suspend fun getVideo(id: Int): BloggerVideoResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/bloggers/video/$id").body()

    suspend fun subscribe(id: Int): BloggerSubscriptionResponseDto =
        clientProvider.get().put("$YANI_BASE_URL/bloggers/$id/subscribe").body()

    suspend fun unsubscribe(id: Int): BloggerSubscriptionResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/bloggers/$id/subscribe").body()

    suspend fun vote(id: Int, action: String): BloggerVideoReactionResponseDto =
        clientProvider.get().put("$YANI_BASE_URL/bloggers/video/$id/vote") {
            contentType(ContentType.Application.Json)
            setBody(BloggerVideoVoteBodyDto(action))
        }.body()

    suspend fun removeVote(id: Int): BloggerVideoReactionResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/bloggers/video/$id/vote").body()
}
