package su.afk.yummy.tv.data.account.dto.social

import su.afk.yummy.tv.data.account.dto.stats.YaniUserWatchHistoryDayDto
import su.afk.yummy.tv.data.account.dto.stats.YaniUserWatchTypeDto
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
