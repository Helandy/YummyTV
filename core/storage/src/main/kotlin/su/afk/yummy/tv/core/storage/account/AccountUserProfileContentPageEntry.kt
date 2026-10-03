package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_profile_content_pages",
    primaryKeys = ["userId", "language", "contentType", "limit", "offset"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_user_profile_content_pages_cachedAt"),
        Index(
            value = ["userId", "language", "contentType"],
            name = "index_account_user_profile_content_pages_scope",
        ),
    ],
)
data class AccountUserProfileContentPageEntry(
    val userId: Int,
    val language: String,
    val contentType: String,
    val limit: Int,
    val offset: Int,
    val cachedAt: Long,
)
