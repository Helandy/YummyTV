package su.afk.yummy.tv.data.account.dto.stats

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserGenreStatDto(
    val id: Int = 0,
    val title: String = "",
    val count: Int = 0,
)
