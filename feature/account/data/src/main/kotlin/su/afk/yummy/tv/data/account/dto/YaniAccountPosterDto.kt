package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniAccountPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val huge: String? = null,
    val fullsize: String? = null,
    val mega: String? = null,
)
