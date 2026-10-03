package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniErrorResponseDto(
    val error: String = "",
    @SerialName("error_title") val errorTitle: String = "",
    @SerialName("error_code") val errorCode: Int? = null,
    @SerialName("error_name") val errorName: String = "",
)
