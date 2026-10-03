package su.afk.yummy.tv.data.account.dto.social

import su.afk.yummy.tv.data.account.dto.common.YaniAccountPosterDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserReviewAnimeDto(
    @SerialName("anime_id") val animeId: Int = 0,
    val title: String = "",
    val poster: YaniAccountPosterDto? = null,
)
