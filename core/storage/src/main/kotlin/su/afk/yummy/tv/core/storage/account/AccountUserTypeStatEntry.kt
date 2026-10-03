package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_type_stats",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_type_stats_userId_language"
        ),
    ],
)
data class AccountUserTypeStatEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val typeId: Int,
    val title: String,
    val shortName: String,
    val count: Int,
)
