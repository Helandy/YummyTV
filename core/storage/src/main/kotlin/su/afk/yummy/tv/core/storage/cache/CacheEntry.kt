package su.afk.yummy.tv.core.storage.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache")
data class CacheEntry(
    @PrimaryKey val key: String,
    val json: String,
    val cachedAt: Long,
)
