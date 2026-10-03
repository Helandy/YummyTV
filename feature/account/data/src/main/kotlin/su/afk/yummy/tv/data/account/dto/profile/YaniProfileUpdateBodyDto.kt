package su.afk.yummy.tv.data.account.dto.profile

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniProfileUpdateBodyDto(
    val about: String,
    @SerialName("bdate") val birthDate: String,
    val sex: Int,
    @SerialName("lists_privacy") val listsPrivacy: String,
    val hide: YaniProfileHideBodyDto,
    val notifications: YaniProfileNotificationsBodyDto,
)
