package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_notification_anime",
    primaryKeys = ["slug"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_notification_anime_cachedAt"),
    ],
)
data class AccountNotificationAnimeEntry(
    val slug: String,
    val animeId: Int? = null,
    val cachedAt: Long,
)
