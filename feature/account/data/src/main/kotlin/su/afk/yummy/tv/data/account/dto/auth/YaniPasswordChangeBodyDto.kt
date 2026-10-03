package su.afk.yummy.tv.data.account.dto.auth

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniPasswordChangeBodyDto(
    @SerialName("old_password") val oldPassword: String,
    @SerialName("new_password") val newPassword: String,
    @EncodeDefault @SerialName("need_json") val needJson: Boolean = true,
)
