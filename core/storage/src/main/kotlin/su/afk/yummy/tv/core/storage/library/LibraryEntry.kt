package su.afk.yummy.tv.core.storage.library

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library")
data class LibraryEntry(
    @PrimaryKey val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val addedAt: Long = System.currentTimeMillis(),
)
