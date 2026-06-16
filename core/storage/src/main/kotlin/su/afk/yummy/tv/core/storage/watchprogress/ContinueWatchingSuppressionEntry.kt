package su.afk.yummy.tv.core.storage.watchprogress

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "continue_watching_suppressions",
    indices = [
        Index(value = ["suppressedAt"], name = "index_continue_watching_suppressions_suppressedAt"),
    ],
)
data class ContinueWatchingSuppressionEntry(
    @PrimaryKey val animeId: Int,
    val suppressedAt: Long = System.currentTimeMillis(),
)
