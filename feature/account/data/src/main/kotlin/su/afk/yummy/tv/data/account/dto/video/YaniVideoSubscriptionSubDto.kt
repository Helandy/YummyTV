package su.afk.yummy.tv.data.account.dto.video

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** `dubbing` — не озвучка подписки, а перечисление всех озвучек плеера либо пустая строка. */
@Serializable
data class YaniVideoSubscriptionSubDto(
    val player: String = "",
    @SerialName("player_id") val playerId: JsonElement? = null,
    val dubbing: String = "",
)
