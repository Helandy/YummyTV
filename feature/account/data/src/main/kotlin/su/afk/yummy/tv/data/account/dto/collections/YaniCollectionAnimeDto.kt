package su.afk.yummy.tv.data.account.dto.collections

import su.afk.yummy.tv.data.account.dto.common.YaniAccountPosterDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniCollectionAnimeDto(
    val poster: YaniAccountPosterDto? = null,
)
