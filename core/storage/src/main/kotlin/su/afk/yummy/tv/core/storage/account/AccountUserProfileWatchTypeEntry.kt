package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_profile_watch_types",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_profile_watch_types_userId_language",
        ),
    ],
)
data class AccountUserProfileWatchTypeEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val typeId: Int,
    val alias: String,
    val title: String,
    val shortName: String,
    val spentSeconds: Long,
)
