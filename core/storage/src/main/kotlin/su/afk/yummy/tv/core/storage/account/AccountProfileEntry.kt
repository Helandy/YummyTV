package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_profiles",
    primaryKeys = ["profileKey"],
    indices = [
        Index(value = ["userId"], name = "index_account_profiles_userId"),
        Index(value = ["cachedAt"], name = "index_account_profiles_cachedAt"),
    ],
)
data class AccountProfileEntry(
    val profileKey: String,
    val userId: Int,
    val nickname: String,
    val avatarUrl: String? = null,
    val cachedAt: Long,
)
