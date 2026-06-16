package su.afk.yummy.tv.core.storage.library

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_sync_states")
data class LibrarySyncStateEntry(
    @PrimaryKey val userId: Int,
    val syncedAt: Long = System.currentTimeMillis(),
)
