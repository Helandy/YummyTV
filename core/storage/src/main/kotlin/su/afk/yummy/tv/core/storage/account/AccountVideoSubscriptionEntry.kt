package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_video_subscriptions",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_video_subscriptions_userId_language",
        ),
    ],
)
data class AccountVideoSubscriptionEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val animeId: Int,
    val animeUrl: String,
    val playerId: Int? = null,
    val player: String,
    val dubbing: String,
    val posterUrl: String? = null,
    val title: String,
)
