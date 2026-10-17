package su.afk.yummy.tv.core.storage.watchlater

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLaterDao {

    @Query("SELECT * FROM watch_later ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchLaterEntry>>

    @Query("SELECT * FROM watch_later WHERE animeId = :animeId")
    fun observeByAnimeId(animeId: Int): Flow<List<WatchLaterEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: WatchLaterEntry)

    @Query("DELETE FROM watch_later WHERE animeId = :animeId AND episode = :episode")
    suspend fun delete(animeId: Int, episode: String)

    @Query("DELETE FROM watch_later WHERE animeId = :animeId")
    suspend fun deleteByAnimeId(animeId: Int)
}
