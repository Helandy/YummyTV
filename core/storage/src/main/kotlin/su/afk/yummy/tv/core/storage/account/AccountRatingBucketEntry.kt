package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_rating_buckets",
    primaryKeys = ["animeId", "position"],
    indices = [
        Index(value = ["animeId"], name = "index_account_rating_buckets_animeId"),
    ],
)
data class AccountRatingBucketEntry(
    val animeId: Int,
    val position: Int,
    val rating: Int,
    val count: Int,
)
