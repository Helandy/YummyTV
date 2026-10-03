package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_ratings",
    primaryKeys = ["userId", "animeId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_user_ratings_cachedAt"),
    ],
)
data class AccountUserRatingEntry(
    val userId: Int,
    val animeId: Int,
    val rating: Int? = null,
    val cachedAt: Long,
)
