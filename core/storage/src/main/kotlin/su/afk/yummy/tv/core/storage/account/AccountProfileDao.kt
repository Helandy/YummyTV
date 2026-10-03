package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountProfileDao {

    @Query("SELECT * FROM account_profiles WHERE profileKey = :profileKey LIMIT 1")
    abstract suspend fun getProfile(profileKey: String): AccountProfileEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertProfile(entry: AccountProfileEntry)

    @Query("DELETE FROM account_profiles WHERE profileKey = :profileKey")
    abstract suspend fun deleteProfile(profileKey: String)

    @Query("DELETE FROM account_profiles WHERE userId = :userId")
    abstract suspend fun deleteProfilesByUser(userId: Int)
}
