package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountUserListsDao {

    @Query(
        """
        SELECT * FROM account_user_list_pages
        WHERE userId = :userId AND listId = :listId AND language = :language
        LIMIT 1
        """
    )
    abstract suspend fun getUserListPageEntry(
        userId: Int,
        listId: Int,
        language: String,
    ): AccountUserListPageEntry?

    @Query(
        """
        SELECT * FROM account_user_list_items
        WHERE userId = :userId AND listId = :listId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getUserListItems(
        userId: Int,
        listId: Int,
        language: String,
    ): List<AccountUserListItemEntry>

    @Query("SELECT EXISTS(SELECT 1 FROM account_user_list_pages WHERE userId = :userId)")
    abstract suspend fun hasUserListPages(userId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserListPage(entry: AccountUserListPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserListItems(entries: List<AccountUserListItemEntry>)

    @Query("DELETE FROM account_user_list_pages WHERE userId = :userId")
    abstract suspend fun deleteUserListPages(userId: Int)

    @Query("DELETE FROM account_user_list_items WHERE userId = :userId")
    abstract suspend fun deleteUserListItems(userId: Int)

    @Query(
        """
        DELETE FROM account_user_list_pages
        WHERE userId = :userId AND listId = :listId AND language = :language
        """
    )
    abstract suspend fun deleteUserListPage(userId: Int, listId: Int, language: String)

    @Query(
        """
        DELETE FROM account_user_list_items
        WHERE userId = :userId AND listId = :listId AND language = :language
        """
    )
    abstract suspend fun deleteUserListPageItems(userId: Int, listId: Int, language: String)

    @Query("SELECT * FROM account_anime_list_states WHERE userId = :userId AND animeId = :animeId LIMIT 1")
    abstract suspend fun getAnimeListState(userId: Int, animeId: Int): AccountAnimeListStateEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAnimeListState(entry: AccountAnimeListStateEntry)

    @Query("DELETE FROM account_anime_list_states WHERE userId = :userId")
    abstract suspend fun deleteAnimeListStates(userId: Int)

    @Transaction
    open suspend fun getUserList(
        userId: Int,
        listId: Int,
        language: String,
    ): AccountUserListCache? {
        val entry = getUserListPageEntry(userId, listId, language) ?: return null
        return AccountUserListCache(entry, getUserListItems(userId, listId, language))
    }

    @Transaction
    open suspend fun replaceUserList(cache: AccountUserListCache) {
        val entry = cache.entry
        deleteUserListPage(entry.userId, entry.listId, entry.language)
        deleteUserListPageItems(entry.userId, entry.listId, entry.language)
        insertUserListPage(entry)
        if (cache.items.isNotEmpty()) insertUserListItems(cache.items)
    }

    @Transaction
    open suspend fun replaceUserLists(caches: List<AccountUserListCache>) {
        caches.forEach { cache -> replaceUserList(cache) }
    }

    @Transaction
    open suspend fun deleteUserLists(userId: Int) {
        deleteUserListPages(userId)
        deleteUserListItems(userId)
    }
}
