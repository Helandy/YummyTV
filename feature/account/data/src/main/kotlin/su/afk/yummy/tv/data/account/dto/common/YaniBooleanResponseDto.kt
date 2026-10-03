package su.afk.yummy.tv.data.account.dto.common

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniBooleanResponseDto(
    val response: Boolean = false,
)
