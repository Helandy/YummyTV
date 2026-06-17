package su.afk.yummy.tv.data.comments.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniCommentsResponseDto(
    val response: YaniCommentsPayloadDto = YaniCommentsPayloadDto(),
)

@Serializable
data class YaniCommentsPayloadDto(
    val comments: List<YaniCommentDto> = emptyList(),
    @SerialName("isMod") val isModerator: Boolean = false,
)

@Serializable
data class YaniCommentResponseDto(
    val response: YaniCommentDto = YaniCommentDto(),
)

@Serializable
data class YaniCommentDto(
    val id: Int = 0,
    val name: String = "",
    val avatars: YaniCommentAvatarsDto = YaniCommentAvatarsDto(),
    @SerialName("children_count") val childrenCount: Int = 0,
    @SerialName("deleted_at") val deletedAt: Long = 0,
    val time: Long = 0,
    @SerialName("user_id") val userId: Int = 0,
    val vote: Int? = null,
    val roles: List<String> = emptyList(),
    val text: String = "",
    val dislikes: Int = 0,
    val likes: Int = 0,
    @SerialName("parent_id") val parentId: Int = 0,
)

@Serializable
data class YaniCommentAvatarsDto(
    val small: String? = null,
    val big: String? = null,
    val full: String? = null,
)

@Serializable
data class YaniPostCommentBodyDto(
    val text: String,
    @SerialName("parent_comment") val parentComment: Int? = null,
    @SerialName("reply_to_comment") val replyToComment: Int? = null,
)

@Serializable
data class YaniPatchCommentBodyDto(
    val text: String,
)

@Serializable
class YaniDeleteCommentBodyDto

@Serializable
data class YaniVoteCommentBodyDto(
    val action: Int,
)

@Serializable
data class YaniVoteCommentResponseDto(
    val response: YaniVoteCommentPayloadDto = YaniVoteCommentPayloadDto(),
)

@Serializable
data class YaniVoteCommentPayloadDto(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val success: Boolean = false,
)

@Serializable
data class YaniClaimCommentBodyDto(
    val why: Int,
)

@Serializable
data class YaniBooleanResponseDto(
    val response: Boolean = false,
)
