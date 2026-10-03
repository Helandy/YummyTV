package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniVideoSubscriptionDto(
    @SerialName("anime_id") val animeId: JsonElement? = null,
    @SerialName("anime_url") val animeUrl: String = "",
    val title: String = "",
    val poster: YaniAccountPosterDto? = null,
    val sub: YaniVideoSubscriptionSubDto? = null,
)
