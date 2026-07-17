package su.afk.yummy.tv.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniPostCategoriesResponseDto(val response: List<YaniPostCategoryDto> = emptyList())

@Serializable
data class YaniPostsResponseDto(val response: List<YaniPostSummaryDto> = emptyList())

@Serializable
data class YaniPostDetailsResponseDto(val response: YaniPostDetailsDto = YaniPostDetailsDto())

@Serializable
data class YaniPostVoteResponseDto(val response: YaniPostVoteResultDto = YaniPostVoteResultDto())

@Serializable
data class YaniPostCategoryDto(val id: Int = 0, val title: String = "", val uri: String = "")

@Serializable
data class YaniPostAvatarDto(
    val small: String? = null,
    val big: String? = null,
    val full: String? = null
)

@Serializable
data class YaniPostAuthorDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: YaniPostAvatarDto? = null
)

@Serializable
data class YaniPostSummaryDto(
    val id: Int = 0,
    val title: String = "",
    @SerialName("preview_image") val previewImage: String? = null,
    @SerialName("content_preview") val contentPreview: String = "",
    val user: YaniPostAuthorDto = YaniPostAuthorDto(),
    val category: YaniPostCategoryDto = YaniPostCategoryDto(),
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class YaniPostLikesDto(val likes: Int = 0, val dislikes: Int = 0, val vote: Int = 0)

@Serializable
data class YaniPostPosterDto(
    val small: String? = null, val medium: String? = null, val big: String? = null,
    val huge: String? = null, val mega: String? = null,
)

@Serializable
data class YaniPostRatingDto(val average: Double? = null)

@Serializable
data class YaniPostAnimeDto(
    @SerialName("anime_id") val animeId: Int = 0,
    val title: String = "",
    val poster: YaniPostPosterDto? = null,
    val year: Int? = null,
    val rating: YaniPostRatingDto? = null,
)

@Serializable
data class YaniPostDetailsDto(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    @SerialName("preview_image") val previewImage: String? = null,
    val user: YaniPostAuthorDto = YaniPostAuthorDto(),
    val category: YaniPostCategoryDto = YaniPostCategoryDto(),
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("edited_at") val editedAt: Long? = null,
    val animes: List<YaniPostAnimeDto> = emptyList(),
    val likes: YaniPostLikesDto = YaniPostLikesDto(),
    val views: Int = 0,
    val comments: Int = 0,
)

@Serializable
data class YaniPostVoteBodyDto(val action: Int)

@Serializable
data class YaniPostVoteResultDto(val likes: Int = 0, val dislikes: Int = 0)
