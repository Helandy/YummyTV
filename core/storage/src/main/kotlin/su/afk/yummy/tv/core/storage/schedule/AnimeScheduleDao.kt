package su.afk.yummy.tv.core.storage.schedule

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AnimeScheduleDao {

    @Query("SELECT * FROM anime_schedule_caches WHERE language = :language LIMIT 1")
    abstract suspend fun getCacheEntry(language: String): AnimeScheduleCacheEntry?

    @Query("SELECT * FROM anime_schedule_items WHERE language = :language ORDER BY position")
    abstract suspend fun getItems(language: String): List<AnimeScheduleItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCache(entry: AnimeScheduleCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(entries: List<AnimeScheduleItemEntry>)

    @Query("DELETE FROM anime_schedule_caches WHERE language = :language")
    abstract suspend fun deleteCache(language: String)

    @Query("DELETE FROM anime_schedule_items WHERE language = :language")
    abstract suspend fun deleteItems(language: String)

    @Transaction
    open suspend fun getSchedule(language: String): AnimeScheduleCache? {
        val entry = getCacheEntry(language) ?: return null
        return AnimeScheduleCache(
            entry = entry,
            items = getItems(language),
        )
    }

    @Transaction
    open suspend fun replaceSchedule(cache: AnimeScheduleCache) {
        val language = cache.entry.language
        deleteCache(language)
        deleteItems(language)

        insertCache(cache.entry)
        if (cache.items.isNotEmpty()) insertItems(cache.items)
    }
}
