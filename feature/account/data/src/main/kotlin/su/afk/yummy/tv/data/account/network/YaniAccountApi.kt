package su.afk.yummy.tv.data.account.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.network.yani.YANI_BASE_URL
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.account.dto.lists.YaniAnimeListStatDto
import su.afk.yummy.tv.data.account.dto.lists.YaniAnimeListStateDto
import su.afk.yummy.tv.data.account.dto.lists.YaniAnimeListStateResponseDto
import su.afk.yummy.tv.data.account.dto.lists.YaniAnimeListStatsResponseDto
import su.afk.yummy.tv.data.account.dto.rating.YaniAnimeUserRatingDto
import su.afk.yummy.tv.data.account.dto.rating.YaniAnimeUserRatingResponseDto
import su.afk.yummy.tv.data.account.dto.common.YaniBooleanResponseDto
import su.afk.yummy.tv.data.account.dto.collections.YaniCollectionSummaryDto
import su.afk.yummy.tv.data.account.dto.collections.YaniCollectionsResponseDto
import su.afk.yummy.tv.data.account.dto.video.YaniDeleteVideosBodyDto
import su.afk.yummy.tv.data.account.dto.common.YaniErrorResponseDto
import su.afk.yummy.tv.data.account.dto.social.YaniFriendshipResponseDto
import su.afk.yummy.tv.data.account.dto.auth.YaniLoginBodyDto
import su.afk.yummy.tv.data.account.dto.auth.YaniLoginResponseDto
import su.afk.yummy.tv.data.account.dto.notifications.YaniNotificationAnimeResponseDto
import su.afk.yummy.tv.data.account.dto.notifications.YaniNotificationCountsResponseDto
import su.afk.yummy.tv.data.account.dto.notifications.YaniNotificationsResponseDto
import su.afk.yummy.tv.data.account.dto.profile.YaniOnlineBodyDto
import su.afk.yummy.tv.data.account.dto.auth.YaniPasswordChangeBodyDto
import su.afk.yummy.tv.data.account.dto.auth.YaniPasswordChangeResponseDto
import su.afk.yummy.tv.data.account.dto.auth.YaniPasswordResetBodyDto
import su.afk.yummy.tv.data.account.dto.video.YaniPostVideoBodyDto
import su.afk.yummy.tv.data.account.dto.video.YaniPostVideoItemDto
import su.afk.yummy.tv.data.account.dto.profile.YaniProfileDto
import su.afk.yummy.tv.data.account.dto.profile.YaniProfileResponseDto
import su.afk.yummy.tv.data.account.dto.profile.YaniProfileUpdateBodyDto
import su.afk.yummy.tv.data.account.dto.video.YaniPutVideoBodyDto
import su.afk.yummy.tv.data.account.dto.rating.YaniRateBodyDto
import su.afk.yummy.tv.data.account.dto.rating.YaniRatingBucketDto
import su.afk.yummy.tv.data.account.dto.rating.YaniRatingResponseDto
import su.afk.yummy.tv.data.account.dto.auth.YaniRegistrationBodyDto
import su.afk.yummy.tv.data.account.dto.auth.YaniRegistrationVerifyBodyDto
import su.afk.yummy.tv.data.account.dto.auth.YaniRegistrationVerifyResponseDto
import su.afk.yummy.tv.data.account.dto.lists.YaniSetFavoriteBodyDto
import su.afk.yummy.tv.data.account.dto.lists.YaniSetListBodyDto
import su.afk.yummy.tv.data.account.dto.common.YaniSuccessObjectResponseDto
import su.afk.yummy.tv.data.account.dto.auth.YaniTokenResponseDto
import su.afk.yummy.tv.data.account.dto.auth.YaniUnlinkAccountResponseDto
import su.afk.yummy.tv.data.account.dto.lists.YaniUserAnimeDto
import su.afk.yummy.tv.data.account.dto.collections.YaniUserCollectionsResponseDto
import su.afk.yummy.tv.data.account.dto.social.YaniUserFriendsResponseDto
import su.afk.yummy.tv.data.account.dto.lists.YaniUserListResponseDto
import su.afk.yummy.tv.data.account.dto.social.YaniUserPostsResponseDto
import su.afk.yummy.tv.data.account.dto.social.YaniUserProfileResponseDto
import su.afk.yummy.tv.data.account.dto.social.YaniUserReviewsResponseDto
import su.afk.yummy.tv.data.account.dto.stats.YaniUserStatsGenresResponseDto
import su.afk.yummy.tv.data.account.dto.stats.YaniUserStatsListsResponseDto
import su.afk.yummy.tv.data.account.dto.stats.YaniUserStatsRatingsResponseDto
import su.afk.yummy.tv.data.account.dto.stats.YaniUserStatsTypesResponseDto
import su.afk.yummy.tv.data.account.dto.social.YaniUsersResponseDto
import su.afk.yummy.tv.data.account.dto.video.YaniVideoSubscriptionsResponseDto
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider

class YaniCaptchaRequiredException : RuntimeException("Captcha required")
class YaniAccountException(message: String, val code: Int? = null) : RuntimeException(message)

class YaniAccountApi(
    private val clientProvider: YaniHttpClientProvider,
    private val analyticsTracker: AnalyticsTracker,
) {

    suspend fun unlinkAccount(provider: LinkedAccountProvider): Boolean {
        val response = clientProvider.get()
            .delete("$YANI_BASE_URL/profile/login/${provider.apiValue}")
            .body<YaniUnlinkAccountResponseDto>()
            .response
        return (response as? JsonPrimitive)?.booleanOrNull != false
    }

    suspend fun login(login: String, password: String, captchaResponse: String? = null): String {
        val response = try {
            analyticsTracker.log(TAG) { "POST /profile/login" }
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
            analyticsTracker.log(TAG) { "POST /profile/login failed status=${e.response.status.value}" }
            if (e.response.status.value == CAPTCHA_ERROR_CODE) throw YaniCaptchaRequiredException()
            throw e
        }
        analyticsTracker.log(TAG) { "POST /profile/login status=${response.status.value}" }

        if (!response.status.isSuccess()) {
            val error = response.toYaniError()
            if (error.isCaptchaRequired(response.status)) throw YaniCaptchaRequiredException()
            throw YaniAccountException(
                message = error.error.ifBlank { error.errorTitle.ifBlank { "Could not sign in" } },
                code = error.errorCode,
            )
        }

        return response.body<YaniLoginResponseDto>().response.token
    }

    suspend fun register(body: YaniRegistrationBodyDto) {
        val response = try {
            analyticsTracker.log(TAG) { "POST /users" }
            clientProvider.get().post("$YANI_BASE_URL/users") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (error: ClientRequestException) {
            if (error.response.status.value == CAPTCHA_ERROR_CODE) {
                throw YaniCaptchaRequiredException()
            }
            throw error
        }

        if (!response.status.isSuccess()) {
            val error = response.toYaniError()
            if (error.isCaptchaRequired(response.status)) throw YaniCaptchaRequiredException()
            throw YaniAccountException(
                message = error.error.ifBlank { error.errorTitle.ifBlank { "Could not register user" } },
                code = error.errorCode,
            )
        }

        if (!response.body<YaniSuccessObjectResponseDto>().response.success) {
            throw YaniAccountException("Could not register user")
        }
    }

    suspend fun verifyRegistration(hash: String): String {
        analyticsTracker.log(TAG) { "POST /users/registration-verify" }
        val result = clientProvider.get().post("$YANI_BASE_URL/users/registration-verify") {
            contentType(ContentType.Application.Json)
            setBody(YaniRegistrationVerifyBodyDto(hash = hash))
        }.body<YaniRegistrationVerifyResponseDto>().response
        if (!result.success || result.token.isBlank()) {
            throw YaniAccountException("Could not verify registration")
        }
        return result.token
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

    suspend fun updateOnline(deviceHash: String) {
        analyticsTracker.log(TAG) { "POST /profile/online" }
        val result = clientProvider.get().post("$YANI_BASE_URL/profile/online") {
            contentType(ContentType.Application.Json)
            setBody(YaniOnlineBodyDto(hash = deviceHash))
        }.body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not update online status")
    }

    suspend fun getUserProfile(userId: Int): YaniUserProfileResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/users/id$userId") {
            parameter("need_counts", true)
        }.body()

    suspend fun searchUsers(query: String, limit: Int, offset: Int) =
        clientProvider.get().get("$YANI_BASE_URL/users") {
            parameter("nickname", query)
            parameter("order_by", "a_z")
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniUsersResponseDto>().response.items

    suspend fun getUserProfileByNickname(nickname: String): YaniUserProfileResponseDto =
        clientProvider.get().get("$YANI_BASE_URL/users/$nickname") {
            parameter("need_counts", true)
        }.body()

    suspend fun getFriendshipStatus(userId: Int, friendId: Int): String? =
        try {
            clientProvider.get().get("$YANI_BASE_URL/users/$userId/friends/$friendId")
                .body<YaniFriendshipResponseDto>().response.status
        } catch (error: ClientRequestException) {
            if (error.response.status == HttpStatusCode.NotFound) null else throw error
        }

    suspend fun addFriend(userId: Int, friendId: Int) {
        val result = clientProvider.get().put("$YANI_BASE_URL/users/$userId/friends/$friendId")
            .body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not add friend")
    }

    suspend fun removeFriend(userId: Int, friendId: Int) {
        val result = clientProvider.get().delete("$YANI_BASE_URL/users/$userId/friends/$friendId")
            .body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not remove friend")
    }

    suspend fun logout() {
        clientProvider.get().post("$YANI_BASE_URL/profile/logout")
    }

    suspend fun updateProfile(body: YaniProfileUpdateBodyDto) {
        val result = clientProvider.get().patch("$YANI_BASE_URL/profile") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not update profile")
    }

    suspend fun uploadAvatar(userId: Int, bytes: ByteArray) {
        val result = clientProvider.get().put("$YANI_BASE_URL/users/$userId/avatar") {
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }.body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not upload avatar")
    }

    suspend fun deleteAvatar(userId: Int) {
        val result = clientProvider.get().delete("$YANI_BASE_URL/users/$userId/avatar")
            .body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not delete avatar")
    }

    suspend fun uploadBanner(userId: Int, bytes: ByteArray) {
        val result = clientProvider.get().put("$YANI_BASE_URL/users/$userId/banner") {
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }.body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not upload banner")
    }

    suspend fun deleteBanner(userId: Int) {
        val result = clientProvider.get().delete("$YANI_BASE_URL/users/$userId/banner")
            .body<YaniBooleanResponseDto>().response
        if (!result) throw YaniAccountException("Could not delete banner")
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): String {
        val response = clientProvider.get().patch("$YANI_BASE_URL/profile/password") {
            contentType(ContentType.Application.Json)
            setBody(YaniPasswordChangeBodyDto(oldPassword, newPassword))
        }.body<YaniPasswordChangeResponseDto>().response
        if (!response.success || response.token.isBlank()) {
            throw YaniAccountException("Could not change password")
        }
        return response.token
    }

    suspend fun requestPasswordReset(email: String, captchaResponse: String?) {
        val response = try {
            clientProvider.get().post("$YANI_BASE_URL/profile/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(YaniPasswordResetBodyDto(email, captchaResponse))
            }
        } catch (error: ClientRequestException) {
            if (error.response.status.value == CAPTCHA_ERROR_CODE) throw YaniCaptchaRequiredException()
            throw error
        }
        if (!response.status.isSuccess()) {
            val error = response.toYaniError()
            if (error.isCaptchaRequired(response.status)) throw YaniCaptchaRequiredException()
            throw YaniAccountException(
                message = error.error.ifBlank { error.errorTitle.ifBlank { "Could not request password reset" } },
                code = error.errorCode,
            )
        }
        if (!response.body<YaniSuccessObjectResponseDto>().response.success) {
            throw YaniAccountException("Could not request password reset")
        }
    }

    suspend fun getUserList(userId: Int, listId: Int): List<YaniUserAnimeDto> =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/lists/$listId")
            .body<YaniUserListResponseDto>()
            .response

    suspend fun getAllUserLists(userId: Int): List<YaniUserAnimeDto> =
        clientProvider.get().get("$YANI_BASE_URL/users/$userId/lists")
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

    suspend fun markWatched(
        videoId: Int,
        timeSeconds: Int,
        durationSeconds: Int,
        times: List<Int>,
    ): Boolean =
        clientProvider.get().put("$YANI_BASE_URL/video/$videoId") {
            contentType(ContentType.Application.Json)
            setBody(
                YaniPutVideoBodyDto(
                    time = timeSeconds,
                    duration = durationSeconds,
                    times = times,
                ),
            )
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

    suspend fun getAnimeListStats(animeId: Int): List<YaniAnimeListStatDto> =
        clientProvider.get().get("$YANI_BASE_URL/anime/$animeId/lists")
            .body<YaniAnimeListStatsResponseDto>()
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
        offset: Int,
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

    suspend fun getNotificationCounts() =
        clientProvider.get().get("$YANI_BASE_URL/profile/notifications/counts")
            .body<YaniNotificationCountsResponseDto>()
            .response

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

    suspend fun deleteAllNotifications(): Boolean =
        clientProvider.get().delete("$YANI_BASE_URL/profile/notifications")
            .body<YaniBooleanResponseDto>()
            .response

    /**
     * Бэкенд сигналит о капче то статусом 420, то кодом в теле, то только текстом — проверяем всё.
     */
    private fun YaniErrorResponseDto.isCaptchaRequired(status: HttpStatusCode): Boolean =
        status.value == CAPTCHA_ERROR_CODE ||
            errorCode == CAPTCHA_ERROR_CODE ||
            CAPTCHA_ERROR_MARKERS.any { error.contains(it, ignoreCase = true) }

    private suspend fun HttpResponse.toYaniError(): YaniErrorResponseDto =
        runSuspendCatching {
            YaniApiJson.decodeFromString<YaniErrorResponseDto>(bodyAsText())
        }.getOrElse {
            YaniErrorResponseDto(error = status.description.ifBlank { "Could not sign in" })
        }

    private companion object {
        const val TAG = "YaniAccountApi"
        const val CAPTCHA_ERROR_CODE = 420
        val CAPTCHA_ERROR_MARKERS = listOf("капч", "captcha")
        const val YANI_AUTHORIZATION_PREFIX = "Bearer "
    }
}
