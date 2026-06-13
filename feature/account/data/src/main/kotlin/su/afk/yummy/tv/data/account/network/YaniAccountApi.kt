package su.afk.yummy.tv.data.account.network

import android.util.Log
import io.ktor.client.HttpClient
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeListStateResponseDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserRatingDto
import su.afk.yummy.tv.data.account.dto.YaniAnimeUserRatingResponseDto
import su.afk.yummy.tv.data.account.dto.YaniBooleanResponseDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionSummaryDto
import su.afk.yummy.tv.data.account.dto.YaniCollectionsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniErrorResponseDto
import su.afk.yummy.tv.data.account.dto.YaniListStatDto
import su.afk.yummy.tv.data.account.dto.YaniListStatsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniLoginBodyDto
import su.afk.yummy.tv.data.account.dto.YaniLoginResponseDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationAnimeResponseDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationCountsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniPostVideoItemDto
import su.afk.yummy.tv.data.account.dto.YaniPostVideosBodyDto
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
import su.afk.yummy.tv.data.account.dto.YaniUserListResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsGenresResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsListsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsRatingsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniUserStatsTypesResponseDto
import su.afk.yummy.tv.data.account.dto.YaniVideoSubscriptionsResponseDto

class YaniCaptchaRequiredException : RuntimeException("Captcha required")
class YaniAccountException(message: String, val code: Int? = null) : RuntimeException(message)

class YaniAccountApi(
    private val client: HttpClient,
) {

    suspend fun login(login: String, password: String, captchaResponse: String? = null): String {
        val response = try {
            logDebug { "POST /profile/login" }
            client.post("$YANI_BASE_URL/profile/login") {
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
            logDebug { "POST /profile/login failed status=${e.response.status.value}" }
            if (e.response.status.value == CAPTCHA_ERROR_CODE) throw YaniCaptchaRequiredException()
            throw e
        }
        logDebug { "POST /profile/login status=${response.status.value}" }

        if (!response.status.isSuccess()) {
            val error = response.toYaniError()
            if (response.status == HttpStatusCode(420, "Captcha Required") || error.errorCode == CAPTCHA_ERROR_CODE) {
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
        client.get("$YANI_BASE_URL/profile/token").body<YaniTokenResponseDto>().response.token

    suspend fun getProfile(token: String? = null): YaniProfileDto =
        client.get("$YANI_BASE_URL/profile") {
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, YANI_AUTHORIZATION_PREFIX + token.trim())
            }
        }.body<YaniProfileResponseDto>().response

    suspend fun logout() {
        client.post("$YANI_BASE_URL/profile/logout")
    }

    suspend fun getUserList(userId: Int, listId: Int): List<YaniUserAnimeDto> =
        client.get("$YANI_BASE_URL/users/$userId/lists/$listId")
            .body<YaniUserListResponseDto>()
            .response

    suspend fun getAnimeListState(animeId: Int): YaniAnimeListStateDto =
        client.get("$YANI_BASE_URL/anime/$animeId/list")
            .body<YaniAnimeListStateResponseDto>()
            .response

    suspend fun setAnimeList(animeId: Int, listId: Int) {
        client.put("$YANI_BASE_URL/anime/$animeId/list") {
            contentType(ContentType.Application.Json)
            setBody(YaniSetListBodyDto(listId))
        }
    }

    suspend fun removeAnimeList(animeId: Int) {
        client.delete("$YANI_BASE_URL/anime/$animeId/list")
    }

    suspend fun setFavorite(animeId: Int) {
        client.put("$YANI_BASE_URL/anime/$animeId/list/fav") {
            contentType(ContentType.Application.Json)
            setBody(YaniSetFavoriteBodyDto())
        }
    }

    suspend fun removeFavorite(animeId: Int) {
        client.delete("$YANI_BASE_URL/anime/$animeId/list/fav")
    }

    suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        client.put("$YANI_BASE_URL/video/$videoId") {
            contentType(ContentType.Application.Json)
            setBody(YaniPutVideoBodyDto(time = timeSeconds, duration = durationSeconds))
        }.body<YaniBooleanResponseDto>().response

    suspend fun removeWatched(videoId: Int): Boolean =
        client.delete("$YANI_BASE_URL/video/$videoId").body<YaniBooleanResponseDto>().response

    suspend fun syncWatched(videos: List<YaniPostVideoItemDto>): Boolean =
        client.post("$YANI_BASE_URL/video") {
            contentType(ContentType.Application.Json)
            setBody(YaniPostVideosBodyDto(videos))
        }.body<YaniBooleanResponseDto>().response

    suspend fun getRatingBuckets(animeId: Int): List<YaniRatingBucketDto> =
        client.get("$YANI_BASE_URL/anime/$animeId/rates")
            .body<YaniRatingResponseDto>()
            .response

    suspend fun getUserRating(animeId: Int): YaniAnimeUserRatingDto =
        client.get("$YANI_BASE_URL/anime/$animeId")
            .body<YaniAnimeUserRatingResponseDto>()
            .response

    suspend fun setRating(animeId: Int, rating: Int) {
        client.put("$YANI_BASE_URL/anime/$animeId/rate") {
            contentType(ContentType.Application.Json)
            setBody(YaniRateBodyDto(rating.coerceIn(1, 10)))
        }
    }

    suspend fun deleteRating(animeId: Int) {
        client.delete("$YANI_BASE_URL/anime/$animeId/rate")
    }

    suspend fun getListStats(animeId: Int): List<YaniListStatDto> =
        client.get("$YANI_BASE_URL/anime/$animeId/lists")
            .body<YaniListStatsResponseDto>()
            .response

    suspend fun getAnimeCollections(animeId: Int, limit: Int, offset: Int): List<YaniCollectionSummaryDto> =
        client.get("$YANI_BASE_URL/anime/$animeId/collections") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniCollectionsResponseDto>().response

    suspend fun getCollections(limit: Int, offset: Int): List<YaniCollectionSummaryDto> =
        client.get("$YANI_BASE_URL/collection") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniCollectionsResponseDto>().response

    suspend fun setSubscribed(videoId: Int): Boolean =
        client.put("$YANI_BASE_URL/video/$videoId/subscribe").body<YaniBooleanResponseDto>().response

    suspend fun removeSubscribed(videoId: Int): Boolean =
        client.delete("$YANI_BASE_URL/video/$videoId/subscribe").body<YaniBooleanResponseDto>().response

    suspend fun getSubscriptions(userId: Int) =
        client.get("$YANI_BASE_URL/users/$userId/lists/subs")
            .body<YaniVideoSubscriptionsResponseDto>()
            .response

    suspend fun getUserStatsGenres(userId: Int) =
        client.get("$YANI_BASE_URL/users/$userId/stats/genres")
            .body<YaniUserStatsGenresResponseDto>()
            .response

    suspend fun getUserStatsRatings(userId: Int) =
        client.get("$YANI_BASE_URL/users/$userId/stats/ratings")
            .body<YaniUserStatsRatingsResponseDto>()
            .response

    suspend fun getUserStatsLists(userId: Int) =
        client.get("$YANI_BASE_URL/users/$userId/stats/lists")
            .body<YaniUserStatsListsResponseDto>()
            .response

    suspend fun getUserStatsTypes(userId: Int) =
        client.get("$YANI_BASE_URL/users/$userId/stats/types-v2")
            .body<YaniUserStatsTypesResponseDto>()
            .response

    suspend fun getNotifications(limit: Int, offset: Int) =
        client.get("$YANI_BASE_URL/profile/notifications") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniNotificationsResponseDto>().response

    suspend fun getNotificationAnimeId(slug: String): Int? =
        client.get("$YANI_BASE_URL/anime/$slug")
            .body<YaniNotificationAnimeResponseDto>()
            .response
            .animeId

    suspend fun getNotificationCounts() =
        client.get("$YANI_BASE_URL/profile/notifications/counts")
            .body<YaniNotificationCountsResponseDto>()
            .response

    suspend fun markNotificationRead(id: Int): Boolean =
        client.post("$YANI_BASE_URL/profile/notifications/$id/read")
            .body<YaniBooleanResponseDto>()
            .response

    suspend fun markAllNotificationsRead(): Boolean {
        val response = client.post("$YANI_BASE_URL/profile/notifications/read") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }
        return response.body<YaniBooleanResponseDto>().response
    }

    suspend fun deleteNotification(id: Int): Boolean =
        client.delete("$YANI_BASE_URL/profile/notifications/$id")
            .body<YaniBooleanResponseDto>()
            .response

    private suspend fun HttpResponse.toYaniError(): YaniErrorResponseDto =
        runCatching {
            ERROR_JSON.decodeFromString<YaniErrorResponseDto>(bodyAsText())
        }.getOrElse {
            YaniErrorResponseDto(error = status.description.ifBlank { "Could not sign in" })
        }

    private fun logDebug(message: () -> String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.println(Log.DEBUG, TAG, message())
        }
    }

    private companion object {
        const val TAG = "YaniAccountApi"
        const val CAPTCHA_ERROR_CODE = 420
        const val YANI_AUTHORIZATION_PREFIX = "Bearer "
        val ERROR_JSON = Json { ignoreUnknownKeys = true }
    }
}
