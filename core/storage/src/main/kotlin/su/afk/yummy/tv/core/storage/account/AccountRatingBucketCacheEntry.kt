package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_rating_bucket_caches",
    primaryKeys = ["animeId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_rating_bucket_caches_cachedAt"),
    ],
)
data class AccountRatingBucketCacheEntry(
    val animeId: Int,
    val cachedAt: Long,
)
