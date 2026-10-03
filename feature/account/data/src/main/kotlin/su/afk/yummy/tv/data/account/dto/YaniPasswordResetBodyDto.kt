package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniPasswordResetBodyDto(
    val email: String,
    @SerialName("recaptcha_response") val captchaResponse: String? = null,
)
