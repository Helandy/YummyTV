package su.afk.yummy.tv.data.account.dto.social

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserProfileResponseDto(
    val response: YaniUserProfileDto = YaniUserProfileDto(),
)
