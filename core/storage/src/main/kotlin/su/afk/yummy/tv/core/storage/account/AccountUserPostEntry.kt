package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_posts",
    primaryKeys = ["userId", "language", "limit", "offset", "position"],
    indices = [
        Index(
            value = ["userId", "language", "limit", "offset"],
            name = "index_account_user_posts_page",
        ),
    ],
)
data class AccountUserPostEntry(
    val userId: Int,
    val language: String,
    val limit: Int,
    val offset: Int,
    val position: Int,
    val postId: Int,
    val title: String,
    val previewImageUrl: String? = null,
    val contentPreview: String,
    val categoryTitle: String,
    val createdAtSeconds: Long,
)
