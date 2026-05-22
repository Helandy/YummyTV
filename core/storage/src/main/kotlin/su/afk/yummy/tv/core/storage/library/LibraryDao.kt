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

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE animeId = :animeId)")
    fun observeIsInLibrary(animeId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: LibraryEntry)

    @Query("DELETE FROM library WHERE animeId = :animeId")
    suspend fun remove(animeId: Int)
}
