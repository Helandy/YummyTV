package su.afk.yummy.tv.data.account

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.domain.account.AccountRepository
import su.afk.yummy.tv.domain.account.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.AnimeListStats
import su.afk.yummy.tv.domain.account.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.RemoteWatchState
import su.afk.yummy.tv.domain.account.UserAnimeList
import su.afk.yummy.tv.domain.account.UserAnimeListItem
import su.afk.yummy.tv.domain.account.UserListsRepository
import su.afk.yummy.tv.domain.account.VideoSubscriptionRepository
import su.afk.yummy.tv.domain.account.VideoWatchesRepository
import su.afk.yummy.tv.domain.account.YaniAccount

class YaniCaptchaRequiredException : RuntimeException("Captcha required")
class YaniAccountException(message: String, val code: Int? = null) : RuntimeException(message)

class YaniAccountRepository(
    private val client: HttpClient,
    private val settingsStore: SettingsStore,
) : AccountRepository {

    private companion object {
        const val TAG = "YaniAccountApi"
        const val CAPTCHA_ERROR_CODE = 420
        val ERROR_JSON = Json { ignoreUnknownKeys = true }
    }

    override suspend fun login(login: String, password: String): YaniAccount {
        val response = try {
            Log.d(TAG, "POST /profile/login")
            client.post("$YANI_BASE_URL/profile/login") {
                contentType(ContentType.Application.Json)
                setBody(YaniLoginBodyDto(login = login, password = password))
            }
        } catch (e: ClientRequestException) {
            Log.d(TAG, "POST /profile/login failed status=${e.response.status.value}")
            if (e.response.status.value == CAPTCHA_ERROR_CODE) throw YaniCaptchaRequiredException()
            throw e
        }
        Log.d(TAG, "POST /profile/login status=${response.status.value}")

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

        val token = response.body<YaniLoginResponseDto>().response.token
        if (token.isBlank()) error("Empty access token")
        settingsStore.setYaniAccount(accessToken = token, userId = 0, nickname = "", avatarUrl = null)
        val profile = getProfile()
        settingsStore.setYaniAccount(token, profile.id, profile.nickname, profile.avatarUrl)
        return profile
    }

    override suspend fun refreshToken(): YaniAccount? {
        val token = runCatching {
            client.get("$YANI_BASE_URL/profile/token").body<YaniTokenResponseDto>().response.token
        }.getOrNull().orEmpty()
        if (token.isBlank()) return null
        val profile = runCatching { getProfile() }.getOrNull()
        settingsStore.setYaniAccount(token, profile?.id ?: 0, profile?.nickname.orEmpty(), profile?.avatarUrl)
        return profile
    }

    override suspend fun getProfile(): YaniAccount =
        client.get("$YANI_BASE_URL/profile").body<YaniProfileResponseDto>().response.toAccount()

    override suspend fun logout() {
        runCatching { client.post("$YANI_BASE_URL/profile/logout") }
        settingsStore.clearYaniAccount()
    }

    private suspend fun io.ktor.client.statement.HttpResponse.toYaniError(): YaniErrorResponseDto =
        runCatching {
            ERROR_JSON.decodeFromString<YaniErrorResponseDto>(bodyAsText())
        }.getOrElse {
            YaniErrorResponseDto(error = status.description.ifBlank { "Could not sign in" })
        }
}

class YaniUserListsRepository(
    private val client: HttpClient,
) : UserListsRepository {

    override suspend fun getUserList(userId: Int, list: UserAnimeList): List<UserAnimeListItem> =
        client.get("$YANI_BASE_URL/users/$userId/lists/${list.id}")
            .body<YaniUserListResponseDto>()
            .response
            .mapNotNull { it.toUserListItem() }

    override suspend fun getAnimeListState(animeId: Int): UserAnimeListItem? {
        val state = client.get("$YANI_BASE_URL/anime/$animeId/list")
            .body<YaniAnimeListStateResponseDto>()
            .response
        return UserAnimeListItem(
            animeId = animeId,
            title = "",
            posterUrl = null,
            rating = null,
            year = null,
            list = state.list.toUserAnimeList(),
            isFavorite = state.isFavorite,
        )
    }

    override suspend fun setAnimeList(animeId: Int, list: UserAnimeList) {
        client.put("$YANI_BASE_URL/anime/$animeId/list") { setBody(YaniSetListBodyDto(list.id)) }
    }

    override suspend fun removeAnimeList(animeId: Int) {
        client.delete("$YANI_BASE_URL/anime/$animeId/list")
    }

    override suspend fun setFavorite(animeId: Int, favorite: Boolean) {
        if (favorite) {
            client.put("$YANI_BASE_URL/anime/$animeId/list/fav") { setBody(YaniSetFavoriteBodyDto()) }
        } else {
            client.delete("$YANI_BASE_URL/anime/$animeId/list/fav")
        }
    }
}

class YaniVideoWatchesRepository(
    private val client: HttpClient,
) : VideoWatchesRepository {

    override suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        client.put("$YANI_BASE_URL/video/$videoId") {
            setBody(YaniPutVideoBodyDto(time = timeSeconds, duration = durationSeconds))
        }.body<YaniBooleanResponseDto>().response

    override suspend fun removeWatched(videoId: Int): Boolean =
        client.delete("$YANI_BASE_URL/video/$videoId").body<YaniBooleanResponseDto>().response

    override suspend fun syncWatched(states: List<RemoteWatchState>): Boolean =
        client.post("$YANI_BASE_URL/video") {
            setBody(
                YaniPostVideosBodyDto(
                    states.map { YaniPostVideoItemDto(it.videoId, it.timeSeconds) }
                )
            )
        }.body<YaniBooleanResponseDto>().response
}

class YaniAnimeExtrasRepository(
    private val client: HttpClient,
) : AnimeExtrasRepository {

    override suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary =
        AnimeRatingSummary(
            distribution = client.get("$YANI_BASE_URL/anime/$animeId/rates")
                .body<YaniRatingResponseDto>()
                .response
                .map { AnimeRatingBucket(it.rating, it.count) },
            userRating = null,
        )

    override suspend fun setRating(animeId: Int, rating: Int) {
        client.put("$YANI_BASE_URL/anime/$animeId/rate") { setBody(YaniRateBodyDto(rating.coerceIn(1, 10))) }
    }

    override suspend fun deleteRating(animeId: Int) {
        client.delete("$YANI_BASE_URL/anime/$animeId/rate")
    }

    override suspend fun getListStats(animeId: Int): AnimeListStats =
        AnimeListStats(
            client.get("$YANI_BASE_URL/anime/$animeId/lists")
                .body<YaniListStatsResponseDto>()
                .response
                .associate { it.listId to it.count }
        )

    override suspend fun getCollections(animeId: Int, limit: Int, offset: Int): List<AnimeCollectionSummary> =
        client.get("$YANI_BASE_URL/anime/$animeId/collections") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniCollectionsResponseDto>().response.mapNotNull { it.toCollectionSummary() }

    override suspend fun getCollections(limit: Int, offset: Int): List<AnimeCollectionSummary> =
        client.get("$YANI_BASE_URL/collection") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body<YaniCollectionsResponseDto>().response.mapNotNull { it.toCollectionSummary() }
}

class YaniVideoSubscriptionRepository(
    private val client: HttpClient,
) : VideoSubscriptionRepository {
    override suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean =
        if (subscribed) {
            client.put("$YANI_BASE_URL/video/$videoId/subscribe").body<YaniBooleanResponseDto>().response
        } else {
            client.delete("$YANI_BASE_URL/video/$videoId/subscribe").body<YaniBooleanResponseDto>().response
        }
}

private fun YaniProfileDto.toAccount(): YaniAccount =
    YaniAccount(
        id = id,
        nickname = nickname,
        avatarUrl = avatars?.full?.toHttpsUrl() ?: avatars?.big?.toHttpsUrl() ?: avatars?.small?.toHttpsUrl(),
    )

private fun YaniUserAnimeDto.toUserListItem(): UserAnimeListItem? {
    val id = animeId ?: return null
    return UserAnimeListItem(
        animeId = id,
        title = title,
        posterUrl = poster?.bestUrl(),
        rating = rating?.takeIf { it > 0.0 },
        year = year?.takeIf { it > 0 },
        list = user?.list?.list?.id.toUserAnimeList(),
        isFavorite = user?.list?.isFav == true,
    )
}

private fun YaniCollectionSummaryDto.toCollectionSummary(): AnimeCollectionSummary? {
    val id = id ?: return null
    return AnimeCollectionSummary(
        id = id,
        title = title,
        description = description,
        posterUrl = animes.firstOrNull()?.poster?.bestUrl(),
        views = views,
    )
}

private fun Int?.toUserAnimeList(): UserAnimeList? =
    UserAnimeList.entries.firstOrNull { it.id == this }

private fun YaniAccountPosterDto.bestUrl(): String? =
    mega?.toHttpsUrl() ?: huge?.toHttpsUrl() ?: big?.toHttpsUrl() ?: medium?.toHttpsUrl() ?: fullsize?.toHttpsUrl() ?: small?.toHttpsUrl()

private fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}
