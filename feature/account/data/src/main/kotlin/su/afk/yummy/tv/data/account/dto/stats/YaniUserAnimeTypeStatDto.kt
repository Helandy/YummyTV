package su.afk.yummy.tv.data.account.dto.stats

import su.afk.yummy.tv.data.account.dto.lists.YaniAnimeTypeDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserAnimeTypeStatDto(
    val type: YaniAnimeTypeDto? = null,
    val count: Int = 0,
)
