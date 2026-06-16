package su.afk.yummy.tv.core.storage.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM library ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<LibraryEntry>>

    @Query("SELECT * FROM library ORDER BY addedAt DESC")
    suspend fun getAll(): List<LibraryEntry>

    @Query("SELECT * FROM library WHERE animeId = :animeId")
    suspend fun getByAnimeId(animeId: Int): LibraryEntry?

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE animeId = :animeId AND listId >= 0)")
    fun observeIsInLibrary(animeId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE animeId = :animeId AND isFavorite = 1)")
    fun observeIsFavorite(animeId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: LibraryEntry)

    @Query("SELECT EXISTS(SELECT 1 FROM library_sync_states WHERE userId = :userId)")
    suspend fun hasSyncState(userId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncState(entry: LibrarySyncStateEntry)

    @Query("DELETE FROM library WHERE animeId = :animeId")
    suspend fun delete(animeId: Int)
}
