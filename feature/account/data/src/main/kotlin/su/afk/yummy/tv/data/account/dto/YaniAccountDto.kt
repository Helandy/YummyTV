package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class YaniLoginBodyDto(
    val login: String,
    val password: String,
    @SerialName("recaptcha_response") val captchaResponse: String? = null,
    @EncodeDefault
    @SerialName("need_json") val needJson: Boolean = true,
)

@Serializable
data class YaniLoginResponseDto(
    val response: YaniLoginTokenDto = YaniLoginTokenDto(),
)

@Serializable
data class YaniLoginTokenDto(
    val success: Boolean = false,
    val token: String = "",
)

@Serializable
data class YaniErrorResponseDto(
    val error: String = "",
    @SerialName("error_title") val errorTitle: String = "",
    @SerialName("error_code") val errorCode: Int? = null,
    @SerialName("error_name") val errorName: String = "",
)

@Serializable
data class YaniTokenResponseDto(
    val response: YaniTokenDto = YaniTokenDto(),
)

@Serializable
data class YaniTokenDto(
    val token: String = "",
)

@Serializable
data class YaniProfileResponseDto(
    val response: YaniProfileDto = YaniProfileDto(),
)

@Serializable
data class YaniProfileDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: YaniAvatarDto? = null,
)

@Serializable
data class YaniUserProfileResponseDto(
    val response: YaniUserProfileDto = YaniUserProfileDto(),
)

@Serializable
data class YaniUserProfileDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: YaniAvatarDto? = null,
    val banner: YaniUserBannerDto? = null,
    @SerialName("register_date") val registerDate: Long? = null,
    @SerialName("bdate") val birthDate: Long? = null,
    val sex: Int? = null,
    val about: String? = null,
    val watches: YaniUserProfileWatchesDto? = null,
    @SerialName("days_online") val daysOnline: Int? = null,
    val counts: Map<String, Int>? = emptyMap(),
    val friends: YaniUserProfileFriendsDto? = null,
    @SerialName("reviewsCount") val reviewsCount: Int? = null,
    @SerialName("reviews_count") val reviewsCountObject: YaniApprovedCountDto? = null,
    @SerialName("posts_count") val postsCount: YaniApprovedCountDto? = null,
    @SerialName("comments_count") val commentsCount: Int? = null,
    @SerialName("collections_count") val collectionsCount: Int? = null,
)

@Serializable
data class YaniUserBannerDto(
    val cropped: String? = null,
    val full: String? = null,
)

@Serializable
data class YaniUserProfileWatchesDto(
    val sum: List<YaniUserWatchTypeDto> = emptyList(),
    val history: List<YaniUserWatchHistoryDayDto> = emptyList(),
)

@Serializable
data class YaniUserWatchTypeDto(
    val value: Int = 0,
    val alias: String = "",
    val name: String = "",
    val shortname: String = "",
    @SerialName("spent_time") val spentTime: Long = 0L,
)

@Serializable
data class YaniUserWatchHistoryDayDto(
    @SerialName("when") val dateSeconds: Long = 0L,
    val duration: Long = 0L,
    @SerialName("ep_count") val episodeCount: Int = 0,
)

@Serializable
data class YaniUserProfileFriendsDto(
    val friends: Int = 0,
    val requests: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val sentRequests: Int = 0,
)

@Serializable
data class YaniApprovedCountDto(
    val approved: Int = 0,
)

@Serializable
data class YaniAvatarDto(
    val small: String? = null,
    val big: String? = null,
    val full: String? = null,
)

@Serializable
data class YaniBooleanResponseDto(
    val response: Boolean = false,
)

@Serializable
data class YaniVideoSubscriptionsResponseDto(
    val response: List<YaniVideoSubscriptionDto> = emptyList(),
)

@Serializable
data class YaniVideoSubscriptionDto(
    @SerialName("anime_id") val animeId: JsonElement? = null,
    @SerialName("anime_url") val animeUrl: String = "",
    val title: String = "",
    val poster: YaniAccountPosterDto? = null,
    val sub: YaniVideoSubscriptionSubDto? = null,
)

@Serializable
data class YaniVideoSubscriptionSubDto(
    val player: String = "",
    @SerialName("player_id") val playerId: JsonElement? = null,
    val dubbing: String = "",
)

@Serializable
data class YaniUserListResponseDto(
    val response: List<YaniUserAnimeDto> = emptyList(),
)

@Serializable
data class YaniUserAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniAccountPosterDto? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val user: YaniAnimeUserDto? = null,
    val date: Long? = null,
)

@Serializable
data class YaniAccountPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val huge: String? = null,
    val fullsize: String? = null,
    val mega: String? = null,
)

@Serializable
data class YaniAnimeUserDto(
    val list: YaniAnimeUserListDto? = null,
    val rating: Double? = null,
)

@Serializable
data class YaniAnimeUserListDto(
    @SerialName("is_fav") val isFav: Boolean = false,
    val list: YaniListInfoDto? = null,
)

@Serializable
data class YaniListInfoDto(
    val id: Int? = null,
    val title: String = "",
    val href: String = "",
)

@Serializable
data class YaniAnimeListStateResponseDto(
    val response: YaniAnimeListStateDto = YaniAnimeListStateDto(),
)

@Serializable
data class YaniAnimeListStateDto(
    val list: Int? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
)

@Serializable
data class YaniAnimeUserRatingResponseDto(
    val response: YaniAnimeUserRatingDto = YaniAnimeUserRatingDto(),
)

@Serializable
data class YaniAnimeUserRatingDto(
    val user: YaniAnimeUserDto? = null,
)

@Serializable
data class YaniSetListBodyDto(
    val list: Int,
    val date: Long = System.currentTimeMillis() / 1000L,
)

@Serializable
data class YaniSetFavoriteBodyDto(
    val date: Long = System.currentTimeMillis() / 1000L,
)

@Serializable
data class YaniPutVideoBodyDto(
    val time: Int,
    val duration: Int,
    val times: List<Int> = emptyList(),
)

@Serializable
data class YaniRatingResponseDto(
    val response: List<YaniRatingBucketDto> = emptyList(),
)

@Serializable
data class YaniRatingBucketDto(
    val rating: Int = 0,
    val count: Int = 0,
)

@Serializable
data class YaniRateBodyDto(
    val rate: Int,
)

@Serializable
data class YaniCollectionsResponseDto(
    val response: List<YaniCollectionSummaryDto> = emptyList(),
)

@Serializable
data class YaniCollectionSummaryDto(
    val id: Int? = null,
    val title: String = "",
    val description: String = "",
    val animes: List<YaniCollectionAnimeDto> = emptyList(),
    @SerialName("poster_previews") val posterPreviews: List<YaniAccountPosterDto> = emptyList(),
    val views: Int? = null,
)

@Serializable
data class YaniCollectionAnimeDto(
    val poster: YaniAccountPosterDto? = null,
)

@Serializable
data class YaniUserStatsGenresResponseDto(
    val response: List<YaniUserGenreStatDto> = emptyList(),
)

@Serializable
data class YaniUserGenreStatDto(
    val id: Int = 0,
    val title: String = "",
    val count: Int = 0,
)

@Serializable
data class YaniUserStatsRatingsResponseDto(
    val response: List<YaniUserRatingStatDto> = emptyList(),
)

@Serializable
data class YaniUserRatingStatDto(
    val rating: Int = 0,
    val count: Int = 0,
)

@Serializable
data class YaniUserStatsListsResponseDto(
    val response: List<YaniUserListWatchStatDto> = emptyList(),
)

@Serializable
data class YaniUserListWatchStatDto(
    val list: YaniListInfoDto? = null,
    val seconds: Long = 0L,
)

@Serializable
data class YaniUserStatsTypesResponseDto(
    val response: List<YaniUserAnimeTypeStatDto> = emptyList(),
)

@Serializable
data class YaniUserAnimeTypeStatDto(
    val type: YaniAnimeTypeDto? = null,
    val count: Int = 0,
)

@Serializable
data class YaniAnimeTypeDto(
    val name: String = "",
    val shortname: String = "",
    val value: Int = 0,
)

@Serializable
data class YaniNotificationsResponseDto(
    val response: List<YaniNotificationDto> = emptyList(),
)

@Serializable
data class YaniNotificationDto(
    val id: Int = 0,
    val date: Long = 0L,
    @SerialName("text_html") val textHtml: String = "",
    @SerialName("title_html") val titleHtml: String = "",
    @SerialName("click_uri") val clickUri: String = "",
    val type: String = "",
    @SerialName("sub_type") val subType: String = "",
    val viewed: Boolean = false,
    @SerialName("object_id") val objectId: Int? = null,
)

@Serializable
data class YaniNotificationAnimeResponseDto(
    val response: YaniNotificationAnimeDto = YaniNotificationAnimeDto(),
)

@Serializable
data class YaniNotificationAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
)

@Serializable
data class YaniNotificationCountsResponseDto(
    val response: List<YaniNotificationCountDto> = emptyList(),
)

@Serializable
data class YaniNotificationCountDto(
    val type: String = "",
    val count: Int = 0,
)
