package su.afk.yummy.tv.data.account.dto.profile

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniProfileIdsDto(
    val shikimori: JsonElement? = null,
    val vk: JsonElement? = null,
    @SerialName("tg_nickname") val telegramNickname: String? = null,
    val discord: JsonElement? = null,
)
