package su.afk.yummy.tv.data.account.dto.lists

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniListInfoDto(
    val id: Int? = null,
    val title: String = "",
    val href: String = "",
)
