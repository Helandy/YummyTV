package su.afk.yummy.tv.data.bloggers.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BloggerVideosResponseDto(val response: List<BloggerVideoDto> = emptyList())

@Serializable
data class BloggerVideoDto(
    val id: Int,
    val title: String = "",
    val descriptions: BloggerVideoDescriptionsDto = BloggerVideoDescriptionsDto(),
    val previews: BloggerVideoPreviewsDto = BloggerVideoPreviewsDto(),
    @SerialName("iframe_url") val iframeUrl: String = "",
    @SerialName("publish_date") val publishDate: Long = 0,
    val views: Long = 0,
    @SerialName("has_spoiler") val hasSpoiler: Boolean = false,
    val category: BloggerVideoCategoryDto = BloggerVideoCategoryDto(),
    val creator: BloggerDto = BloggerDto(),
    val likes: BloggerVideoReactionDto = BloggerVideoReactionDto(),
    @SerialName("comments_count") val commentsCount: Int = 0,
)

@Serializable
data class BloggerVideoReactionDto(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val vote: Int = 0,
)

@Serializable
data class BloggerVideoDescriptionsDto(val small: String = "", val big: String = "")

@Serializable
data class BloggerVideoPreviewsDto(val small: String? = null, val big: String? = null)

@Serializable
data class BloggerVideoCategoryDto(val id: String = "", val title: String = "")

@Serializable
data class BloggerDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatars: BloggerAvatarsDto = BloggerAvatarsDto(),
)

@Serializable
data class BloggerAvatarsDto(
    val small: String? = null,
    val big: String? = null,
    val full: String? = null
)

@Serializable
data class BloggersResponseDto(val response: BloggerDirectoryDto = BloggerDirectoryDto())

@Serializable
data class BloggerDirectoryDto(
    val categories: List<BloggerVideoCategoryDto> = emptyList(),
    val bloggers: List<BloggerDto> = emptyList(),
)

@Serializable
data class BloggerVideoResponseDto(val response: BloggerVideoDto)

@Serializable
data class BloggerDetailsResponseDto(val response: BloggerDetailsDto)

@Serializable
data class BloggerDetailsDto(
    val id: Int,
    val nickname: String = "",
    val avatars: BloggerAvatarsDto = BloggerAvatarsDto(),
    val subscriptions: Int = 0,
    @SerialName("videos_count") val videosCount: Int = 0,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    val categories: List<BloggerVideoCategoryDto> = emptyList(),
)

@Serializable
data class BloggerSubscriptionResponseDto(val response: BloggerSubscriptionDto)

@Serializable
data class BloggerSubscriptionDto(val subscriptions: Int = 0)

@Serializable
data class BloggerVideoReactionResponseDto(val response: BloggerVideoReactionDto)

@Serializable
data class BloggerVideoVoteBodyDto(val action: String)
