package su.afk.yummy.tv.core.storage.watchprogress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId AND episode = :episode")
    suspend fun get(animeId: Int, episode: String): WatchProgressEntry?

    @Query("SELECT * FROM watch_progress")
    fun observeAll(): Flow<List<WatchProgressEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: WatchProgressEntry)

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId AND episode = :episode")
    suspend fun delete(animeId: Int, episode: String)

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId")
    suspend fun deleteByAnimeId(animeId: Int)
}
