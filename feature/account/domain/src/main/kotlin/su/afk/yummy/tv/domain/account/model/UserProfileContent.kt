package su.afk.yummy.tv.domain.account.model

data class UserFriend(
    val id: Int,
    val nickname: String,
    val avatarUrl: String?,
    val lastOnlineSeconds: Long,
    val status: String,
)

data class UserReviewSummary(
    val id: Int,
    val animeId: Int,
    val animeTitle: String,
    val animePosterUrl: String?,
    val textPreview: String,
    val rating: Double?,
    val likes: Int,
    val dislikes: Int,
    val commentsCount: Int,
    val updatedAtSeconds: Long,
)

data class UserPostSummary(
    val id: Int,
    val title: String,
    val previewImageUrl: String?,
    val contentPreview: String,
    val categoryTitle: String,
    val createdAtSeconds: Long,
)
