package su.afk.yummy.tv.data.account.dto.social

import su.afk.yummy.tv.data.account.dto.common.YaniLikesDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserReviewDto(
    @SerialName("review_id") val reviewId: Int = 0,
    @SerialName("anime_id") val animeId: Int = 0,
    @SerialName("text_preview") val textPreview: String = "",
    val rating: YaniUserReviewRatingDto? = null,
    val likes: YaniLikesDto? = null,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("update_date") val updatedAt: Long = 0L,
    val anime: YaniUserReviewAnimeDto? = null,
)
