package su.afk.yummy.tv.data.account.dto.social

import su.afk.yummy.tv.data.account.dto.notifications.YaniApprovedCountDto
import su.afk.yummy.tv.data.account.dto.profile.YaniAvatarDto
import su.afk.yummy.tv.data.account.dto.profile.YaniUserBannerDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserProfileDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: YaniAvatarDto? = null,
    val banner: YaniUserBannerDto? = null,
    @SerialName("register_date") val registerDate: Long? = null,
    @SerialName("last_online") val lastOnline: Long? = null,
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
