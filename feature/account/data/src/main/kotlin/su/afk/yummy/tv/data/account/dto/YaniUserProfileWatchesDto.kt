package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserProfileWatchesDto(
    val sum: List<YaniUserWatchTypeDto> = emptyList(),
    val history: List<YaniUserWatchHistoryDayDto> = emptyList(),
)
