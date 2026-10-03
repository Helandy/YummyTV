package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniRegistrationBodyDto(
    val email: String,
    val password: String,
    val username: String,
    @SerialName("g-recaptcha-response") val captchaResponse: String? = null,
    val hash: String? = null,
    val shiki: String? = null,
    val vk: String? = null,
)
