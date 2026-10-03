package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniProfileDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: YaniAvatarDto? = null,
    val banner: YaniUserBannerDto? = null,
    val about: String = "",
    @SerialName("bdate") val birthDate: Long? = null,
    val sex: Int? = null,
    @SerialName("lists_privacy") val listsPrivacy: String? = null,
    val privacy: YaniProfilePrivacyDto? = null,
    val notifications: YaniProfileNotificationSettingsDto? = null,
    val ids: YaniProfileIdsDto? = null,
)
