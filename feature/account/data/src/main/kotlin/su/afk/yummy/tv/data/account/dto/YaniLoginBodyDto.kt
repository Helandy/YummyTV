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
