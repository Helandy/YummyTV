package su.afk.yummy.tv.core.storage.document

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_cache",
    indices = [Index("cachedAt")],
)
data class DocumentCacheEntry(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val cachedAt: Long,
)
