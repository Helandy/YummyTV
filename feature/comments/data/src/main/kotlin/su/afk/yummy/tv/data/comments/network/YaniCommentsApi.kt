package su.afk.yummy.tv.data.comments.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.comments.dto.YaniBooleanResponseDto
import su.afk.yummy.tv.data.comments.dto.YaniClaimCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniCommentResponseDto
import su.afk.yummy.tv.data.comments.dto.YaniCommentsResponseDto
import su.afk.yummy.tv.data.comments.dto.YaniDeleteCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniPatchCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniPostCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniVoteCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniVoteCommentResponseDto

class YaniCommentsApi(
    private val client: HttpClient,
) {
    suspend fun getAnimeComments(
        animeId: Int,
        limit: Int,
        skip: Int,
        sort: String,
    ): YaniCommentsResponseDto =
        client.get("$YANI_BASE_URL/comments/anime/$animeId") {
            parameter("limit", limit)
            parameter("skip", skip)
            parameter("sort", sort)
        }.body()

    suspend fun getCommentChildren(
        commentId: Int,
        skip: Int,
    ): YaniCommentsResponseDto =
        client.get("$YANI_BASE_URL/comments/$commentId/children") {
            parameter("skip", skip)
        }.body()

    suspend fun addAnimeComment(
        animeId: Int,
        body: YaniPostCommentBodyDto,
    ): YaniCommentResponseDto =
        client.post("$YANI_BASE_URL/comments/anime/$animeId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun updateComment(
        commentId: Int,
        body: YaniPatchCommentBodyDto,
    ): YaniCommentResponseDto =
        client.patch("$YANI_BASE_URL/comments/$commentId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteComment(commentId: Int): YaniBooleanResponseDto =
        client.delete("$YANI_BASE_URL/comments/$commentId") {
            contentType(ContentType.Application.Json)
            setBody(YaniDeleteCommentBodyDto())
        }.body()

    suspend fun voteComment(
        commentId: Int,
        body: YaniVoteCommentBodyDto,
    ): YaniVoteCommentResponseDto =
        client.put("$YANI_BASE_URL/comments/$commentId/vote") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun removeCommentVote(commentId: Int): YaniVoteCommentResponseDto =
        client.delete("$YANI_BASE_URL/comments/$commentId/vote").body()

    suspend fun reportComment(
        commentId: Int,
        body: YaniClaimCommentBodyDto,
    ): YaniBooleanResponseDto =
        client.put("$YANI_BASE_URL/comments/$commentId/claim") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
}
