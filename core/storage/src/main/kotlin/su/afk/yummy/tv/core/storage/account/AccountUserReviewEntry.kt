package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_reviews",
    primaryKeys = ["userId", "language", "limit", "offset", "position"],
    indices = [
        Index(
            value = ["userId", "language", "limit", "offset"],
            name = "index_account_user_reviews_page",
        ),
    ],
)
data class AccountUserReviewEntry(
    val userId: Int,
    val language: String,
    val limit: Int,
    val offset: Int,
    val position: Int,
    val reviewId: Int,
    val animeId: Int,
    val animeTitle: String,
    val animePosterUrl: String? = null,
    val textPreview: String,
    val rating: Double? = null,
    val likes: Int,
    val dislikes: Int,
    val commentsCount: Int,
    val updatedAtSeconds: Long,
)
