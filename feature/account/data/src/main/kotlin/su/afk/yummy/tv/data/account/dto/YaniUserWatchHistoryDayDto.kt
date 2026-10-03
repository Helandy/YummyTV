package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserWatchHistoryDayDto(
    @SerialName("when") val dateSeconds: Long = 0L,
    val duration: Long = 0L,
    @SerialName("ep_count") val episodeCount: Int = 0,
)
