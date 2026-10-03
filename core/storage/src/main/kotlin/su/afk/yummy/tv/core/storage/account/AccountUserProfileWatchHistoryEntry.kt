package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_profile_watch_history",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_profile_watch_history_userId_language",
        ),
    ],
)
data class AccountUserProfileWatchHistoryEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val dateSeconds: Long,
    val durationSeconds: Long,
    val episodeCount: Int,
)
