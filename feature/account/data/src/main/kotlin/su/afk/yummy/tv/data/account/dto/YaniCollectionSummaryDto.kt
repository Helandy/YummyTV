package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniCollectionSummaryDto(
    val id: Int? = null,
    val title: String = "",
    val description: String = "",
    val animes: List<YaniCollectionAnimeDto> = emptyList(),
    @SerialName("poster_previews") val posterPreviews: List<YaniAccountPosterDto> = emptyList(),
    val views: Int? = null,
)
