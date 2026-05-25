package su.afk.yummy.tv.core.storage.library

import androidx.room.Entity
import androidx.room.PrimaryKey

const val FAVORITE_ONLY_LIST_ID = -1

@Entity(tableName = "library")
data class LibraryEntry(
    @PrimaryKey val animeId: Int,
    val title: String,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val listId: Int = 0,
    val isFavorite: Boolean = false,
)

data class LibraryPoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)
