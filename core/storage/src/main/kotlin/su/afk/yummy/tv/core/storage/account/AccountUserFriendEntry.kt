package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_friends",
    primaryKeys = ["userId", "language", "limit", "offset", "position"],
    indices = [
        Index(
            value = ["userId", "language", "limit", "offset"],
            name = "index_account_user_friends_page",
        ),
    ],
)
data class AccountUserFriendEntry(
    val userId: Int,
    val language: String,
    val limit: Int,
    val offset: Int,
    val position: Int,
    val friendId: Int,
    val nickname: String,
    val avatarUrl: String? = null,
    val lastOnlineSeconds: Long,
    val status: String,
)
