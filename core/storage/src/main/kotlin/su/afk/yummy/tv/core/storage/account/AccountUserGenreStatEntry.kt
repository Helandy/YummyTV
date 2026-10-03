package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_genre_stats",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_genre_stats_userId_language"
        ),
    ],
)
data class AccountUserGenreStatEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val genreId: Int,
    val title: String,
    val count: Int,
)
