package su.afk.yummy.tv.core.storage.outbox

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMutationDao {

    @Insert
    suspend fun insert(entry: PendingMutationEntry): Long

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC")
    suspend fun all(): List<PendingMutationEntry>

    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM pending_mutations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_mutations SET attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun incrementAttempt(id: Long)
}
