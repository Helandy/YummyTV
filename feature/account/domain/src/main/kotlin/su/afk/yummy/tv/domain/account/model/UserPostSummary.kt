package su.afk.yummy.tv.domain.account.model

data class UserPostSummary(
    val id: Int,
    val title: String,
    val previewImageUrl: String?,
    val contentPreview: String,
    val categoryTitle: String,
    val createdAtSeconds: Long,
)
