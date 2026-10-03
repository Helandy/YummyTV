package su.afk.yummy.tv.data.account.dto.notifications

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniNotificationAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
)
