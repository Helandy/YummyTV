package su.afk.yummy.tv.data.account.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import su.afk.yummy.tv.core.logger.AppLogger
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.network.YaniApiJson
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateResponseDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserRatingDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserRatingResponseDto
import su.afk.yummy.tv.data.account.dto.YaniBooleanResponseDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionSummaryDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniDeleteVideosBodyDto
import su.afk.yummy.tv.data.account.dto.YaniErrorResponseDto
import su.afk.yummy.tv.data.account.dto.YaniLoginBodyDto
import su.afk.yummy.tv.data.account.dto.YaniLoginResponseDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationAnimeResponseDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniPostVideoBodyDto
import su.afk.yummy.tv.data.account.dto.YaniPostVideoItemDto
import su.afk.yummy.tv.data.account.dto.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.YaniProfileResponseDto
import su.afk.yummy.tv.data.account.dto.YaniPutVideoBodyDto
import su.afk.yummy.tv.data.account.dto.YaniRateBodyDto
import su.afk.yummy.tv.data.account.dto.YaniRatingBucketDto
import su.afk.yummy.tv.data.account.dto.YaniRatingResponseDto
import su.afk.yummy.tv.data.account.dto.YaniSetFavoriteBodyDto
import su.afk.yummy.tv.data.account.dto.YaniSetListBodyDto
import su.afk.yummy.tv.data.account.dto.YaniTokenResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserAnimeDto
import su.afk.yummy.tv.data.account.dto.YaniUserCollectionsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserFriendsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserListResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserPostsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserProfileResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserReviewsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsGenresResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsListsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsRatingsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsTypesResponseDto
import su.afk.yummy.tv.data.account.dto.YaniVideoSubscriptionsResponseDto

class YaniCaptchaRequiredException : RuntimeException("Captcha required")
class YaniAccountException(message: String, val code: Int? = null) : RuntimeException(message)

class YaniAccountApi(
    private val clientProvider: YaniHttpClientProvider,
) {

    suspend fun login(login: String, password: String, captchaResponse: String? = null): String {
        val response = try {
            AppLogger.d(TAG) { "POST /profile/login" }
            clientProvider.get().post("$YANI_BASE_URL/profile/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    YaniLoginBodyDto(
                        login = login,
                        password = password,
                        captchaResponse = captchaResponse,
                    ),
                )
            }
        } catch (e: ClientRequestException) {
            AppLogger.d(TAG) { "POST /profile/login failed status=${e.response.status.value}" }
            if (e.response.status.value == CAPTCHA_ERROR_CODE) throw YaniCaptchaRequiredException()
            throw e
        }
        AppLogger.d(TAG) { "POST /profile/login status=${response.status.value}" }

        if (!response.status.isSuccess()) {
            val error = response.toYaniError()
            if (response.status == HttpStatusCode(
                    420,
                    "Captcha Required"
                ) || error.errorCode == CAPTCHA_ERROR_CODE
            ) {
                throw YaniCaptchaRequiredException()
            }
            throw YaniAccountException(
                message = error.error.ifBlank { error.errorTitle.ifBlank { "Could not sign in" } },
                code = error.errorCode,
            )
        }

        return response.body<YaniLoginResponseDto>().response.token
    }

    suspend fun refreshToken(): String =
        clientProvider.get().get("$YANI_BASE_URL/profile/token")
            .body<YaniTokenResponseDto>().response.token

    suspend fun getProfile(token: String? = null): YaniProfileDto =
        clientProvider.get().get("$YANI_BASE_URL/profile") {
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, YANI_AUTHORIZATION_PREFIX + token.trim())
            }
        }.body<YaniProfileResponseDto>().response

    suspend fun getUserProfile(userId: Int): YaniUserProfileResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/users/id$userId") {
            parameter("need_counts", true)
        }.body()

    suspend fun logout() {
        clientProvider.get().post("$YANI_BASE_URL/profile/logout")
    }

    suspend fun getUserList(userId: Int, listId: Int): List<YaniUserAnimeDto> =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/lists/$listId")
            .body<YaniUserListResponseDto>()
            .response

    suspend fun getUserFriends(userId: Int, limit: Int, offset: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/friends") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniUserFriendsResponseDto>().response

    suspend fun getUserReviews(userId: Int, limit: Int, offset: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/reviews") {
            parameter("type", "approved")
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniUserReviewsResponseDto>().response

    suspend fun getUserPosts(userId: Int, limit: Int, offset: Int) =
        clientProvider.get().get("$YANI_BASE_URL/posts") {
            parameter("user_id", userId)
            parameter("status", "published")
            parameter("sort", "new")
            parameter("limit", limit)
            parameter("skip", offset)
        }.body<YaniUserPostsResponseDto>().response

    suspend fun getUserCollections(userId: Int, limit: Int, offset: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/collections") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniUserCollectionsResponseDto>().response.orEmpty()

    suspend fun getAnimeListState(animeId: Int): YaniAnimeListStateDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/list")
            .body<YaniAnimeListStateResponseDto>()
            .response

    suspend fun setAnimeList(animeId: Int, listId: Int) {
        clientProvider.get().put("$YANI_BASE_URL/anime/$animeId/list") {
            contentType(ContentType.Application.Json)
            setBody(YaniSetListBodyDto(listId))
        }
    }

    suspend fun removeAnimeList(animeId: Int) {
        clientProvider.get().delete("$YANI_BASE_URL/anime/$animeId/list")
    }

    suspend fun setFavorite(animeId: Int) {
        clientProvider.get().put("$YANI_BASE_URL/anime/$animeId/list/fav") {
            contentType(ContentType.Application.Json)
            setBody(YaniSetFavoriteBodyDto())
        }
    }

    suspend fun removeFavorite(animeId: Int) {
        clientProvider.get().delete("$YANI_BASE_URL/anime/$animeId/list/fav")
    }

    suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        clientProvider.get().put("$YANI_BASE_URL/video/$videoId") {
            contentType(ContentType.Application.Json)
            setBody(YaniPutVideoBodyDto(time = timeSeconds, duration = durationSeconds))
        }.body<YaniBooleanResponseDto>().response

    suspend fun syncWatched(videos: List<YaniPostVideoItemDto>): Boolean =
        clientProvider.get().post("$YANI_BASE_URL/video") {
            contentType(ContentType.Application.Json)
            setBody(YaniPostVideoBodyDto(videos))
        }.body<YaniBooleanResponseDto>().response

    suspend fun removeWatched(videoIds: List<Int>): Boolean =
        clientProvider.get().delete("$YANI_BASE_URL/video") {
            contentType(ContentType.Application.Json)
            setBody(YaniDeleteVideosBodyDto(videoIds))
        }.body<YaniBooleanResponseDto>().response

    suspend fun getRatingBuckets(animeId: Int): List<YaniRatingBucketDto> =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/rates")
            .body<YaniRatingResponseDto>()
            .response

    suspend fun getUserRating(animeId: Int): YaniAnimeUserRatingDto =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId")
            .body<YaniAnimeUserRatingResponseDto>()
            .response

    suspend fun setRating(animeId: Int, rating: Int) {
        clientProvider.get().put("$YANI_BASE_URL/anime/$animeId/rate") {
            contentType(ContentType.Application.Json)
            setBody(YaniRateBodyDto(rating.coerceIn(1, 10)))
        }
    }

    suspend fun deleteRating(animeId: Int) {
        clientProvider.get().delete("$YANI_BASE_URL/anime/$animeId/rate")
    }

    suspend fun getAnimeCollections(
        animeId: Int,
        limit: Int,
        offset: Int
    ): List<YaniCollectionSummaryDto> =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/collections") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniCollectionsResponseDto>().response

    suspend fun setSubscribed(videoId: Int): Boolean =
        clientProvider.get().put("$YANI_BASE_URL/video/$videoId/subscribe")
            .body<YaniBooleanResponseDto>().response

    suspend fun removeSubscribed(videoId: Int): Boolean =
        clientProvider.get().delete("$YANI_BASE_URL/video/$videoId/subscribe")
            .body<YaniBooleanResponseDto>().response

    suspend fun getSubscriptions(userId: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/lists/subs")
            .body<YaniVideoSubscriptionsResponseDto>()
            .response

    suspend fun getUserStatsGenres(userId: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/stats/genres")
            .body<YaniUserStatsGenresResponseDto>()
            .response

    suspend fun getUserStatsRatings(userId: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/stats/ratings")
            .body<YaniUserStatsRatingsResponseDto>()
            .response

    suspend fun getUserStatsLists(userId: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/stats/lists")
            .body<YaniUserStatsListsResponseDto>()
            .response

    suspend fun getUserStatsTypes(userId: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/stats/types-v2")
            .body<YaniUserStatsTypesResponseDto>()
            .response

    suspend fun getNotifications(limit: Int, offset: Int) =
        clientProvider.get().get("$YANI_BASE_URL/profile/notifications") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniNotificationsResponseDto>().response

    suspend fun getNotificationAnimeId(slug: String): Int? =
        clientProvider.get().get("$YANI_BASE_URL/anime/$slug")
            .body<YaniNotificationAnimeResponseDto>()
            .response
            .animeId

    suspend fun markNotificationRead(id: Int): Boolean =
        clientProvider.get().post("$YANI_BASE_URL/profile/notifications/$id/read")
            .body<YaniBooleanResponseDto>()
            .response

    suspend fun markAllNotificationsRead(): Boolean {
        val response = clientProvider.get().post("$YANI_BASE_URL/profile/notifications/read") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }
        return response.body<YaniBooleanResponseDto>().response
    }

    suspend fun deleteNotification(id: Int): Boolean =
        clientProvider.get().delete("$YANI_BASE_URL/profile/notifications/$id")
            .body<YaniBooleanResponseDto>()
            .response

    private suspend fun HttpResponse.toYaniError(): YaniErrorResponseDto =
        runCatching {
            YaniApiJson.decodeFromString<YaniErrorResponseDto>(bodyAsText())
        }.getOrElse {
            YaniErrorResponseDto(error = status.description.ifBlank { "Could not sign in" })
        }

    private companion object {
        const val TAG = "YaniAccountApi"
        const val CAPTCHA_ERROR_CODE = 420
        const val YANI_AUTHORIZATION_PREFIX = "Bearer "
    }
}
