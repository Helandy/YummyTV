package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_notifications",
    primaryKeys = ["userId", "language", "limit", "offset", "position"],
    indices = [
        Index(
            value = ["userId", "language", "limit", "offset"],
            name = "index_account_notifications_page",
        ),
    ],
)
data class AccountNotificationEntry(
    val userId: Int,
    val language: String,
    val limit: Int,
    val offset: Int,
    val position: Int,
    val notificationId: Int,
    val dateSeconds: Long,
    val title: String,
    val text: String,
    val clickUri: String,
    val type: String,
    val subType: String,
    val viewed: Boolean,
    val objectId: Int? = null,
    val animeSlug: String? = null,
    val isNewEpisode: Boolean,
)
