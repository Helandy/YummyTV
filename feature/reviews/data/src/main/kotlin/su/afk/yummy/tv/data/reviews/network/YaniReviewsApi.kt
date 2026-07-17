package su.afk.yummy.tv.data.reviews.network

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
import su.afk.yummy.tv.data.reviews.dto.YaniBooleanResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewVoteBodyDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewVoteResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewsFeedResponseDto
import su.afk.yummy.tv.data.reviews.dto.YaniReviewsPageResponseDto
import javax.inject.Inject

class YaniReviewsApi @Inject constructor(private val clientProvider: YaniHttpClientProvider) {
    suspend fun getReviews(
        sort: String,
        limit: Int,
        offset: Int,
    ): YaniReviewsFeedResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/reviews") {
            parameter("sort", sort)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun getAnimeReviews(
        animeId: Int,
        sort: String,
        limit: Int,
        offset: Int
    ): YaniReviewsPageResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/reviews") {
            parameter("sort", sort); parameter("limit", limit); parameter("offset", offset)
        }.body()

    suspend fun getReview(reviewId: Int): YaniReviewResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/reviews/$reviewId").body()

    suspend fun delete(reviewId: Int): YaniBooleanResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/reviews/$reviewId").body()

    suspend fun vote(reviewId: Int, action: Int): YaniReviewVoteResponseDto =
        clientProvider.get().put("$YANI_BASE_URL/reviews/$reviewId/vote") {
            contentType(ContentType.Application.Json); setBody(YaniReviewVoteBodyDto(action))
        }.body()

    suspend fun removeVote(reviewId: Int): YaniReviewVoteResponseDto =
        clientProvider.get().delete("$YANI_BASE_URL/reviews/$reviewId/vote").body()

}
