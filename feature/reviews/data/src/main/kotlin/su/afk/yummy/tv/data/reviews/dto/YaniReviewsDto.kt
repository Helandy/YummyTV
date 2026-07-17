package su.afk.yummy.tv.data.reviews.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniReviewsPageResponseDto(val response: YaniReviewsPageDto = YaniReviewsPageDto())

@Serializable
data class YaniReviewsFeedResponseDto(val response: List<YaniReviewDto> = emptyList())

@Serializable
data class YaniReviewsPageDto(
    val reviews: List<YaniReviewDto> = emptyList(),
)

@Serializable
data class YaniReviewResponseDto(val response: YaniReviewDto = YaniReviewDto())

@Serializable
data class YaniReviewDto(
    @SerialName("review_id") val reviewId: Int = 0,
    @SerialName("anime_id") val animeId: Int = 0,
    @SerialName("user_id") val userId: Int? = null,
    val nickname: String? = null,
    val type: String = "approved",
    @SerialName("create_date") val createDate: Long = 0,
    @SerialName("update_date") val updateDate: Long = 0,
    val views: Int = 0,
    val author: YaniReviewAuthorDto = YaniReviewAuthorDto(),
    val rating: YaniReviewRatingDto? = null,
    val likes: YaniReviewLikesDto = YaniReviewLikesDto(),
    @SerialName("text_html") val textHtml: String = "",
    @SerialName("text_preview") val textPreview: String = "",
    @SerialName("check_comment") val checkComment: String? = null,
    val commentable: Boolean = false,
    val anime: YaniReviewAnimeDto? = null,
    @SerialName("comments_count") val commentsCount: Int = 0,
)

@Serializable
data class YaniReviewAuthorDto(
    val id: Int? = null,
    val nickname: String? = null,
    val avatars: YaniReviewAvatarDto? = null,
)

@Serializable
data class YaniReviewAvatarDto(
    val small: String? = null,
    val big: String? = null,
    val full: String? = null
)

@Serializable
data class YaniReviewRatingDto(
    val average: JsonElement? = null,
    val category: Map<String, Int>? = null,
)

@Serializable
data class YaniReviewLikesDto(val likes: Int = 0, val dislikes: Int = 0, val vote: Int = 0)

@Serializable
data class YaniReviewAnimeDto(
    val title: String = "",
    val poster: YaniReviewPosterDto? = null,
)

@Serializable
data class YaniReviewPosterDto(
    val fullsize: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val huge: String? = null,
    val mega: String? = null
)

@Serializable
data class YaniBooleanResponseDto(val response: Boolean = false)

@Serializable
data class YaniReviewVoteResponseDto(val response: YaniReviewVoteResultDto = YaniReviewVoteResultDto())

@Serializable
data class YaniReviewVoteResultDto(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val success: Boolean = false
)

@Serializable
data class YaniReviewVoteBodyDto(val action: Int)
