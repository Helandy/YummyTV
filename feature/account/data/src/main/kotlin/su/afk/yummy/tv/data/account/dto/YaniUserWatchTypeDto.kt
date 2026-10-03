package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserWatchTypeDto(
    val value: Int = 0,
    val alias: String = "",
    val name: String = "",
    val shortname: String = "",
    @SerialName("spent_time") val spentTime: Long = 0L,
)
