package su.afk.yummy.tv.core.storage.top

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AnimeTopDao {

    @Query(
        """
        SELECT * FROM anime_top_pages
        WHERE type = :type AND language = :language AND `limit` = :limit AND `offset` = :offset
        LIMIT 1
        """
    )
    abstract suspend fun getPageEntry(
        type: String,
        language: String,
        limit: Int,
        offset: Int,
    ): AnimeTopPageEntry?

    @Query(
        """
        SELECT * FROM anime_top_items
        WHERE type = :type AND language = :language AND `limit` = :limit AND `offset` = :offset
        ORDER BY position
        """
    )
    abstract suspend fun getItems(
        type: String,
        language: String,
        limit: Int,
        offset: Int,
    ): List<AnimeTopItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPage(entry: AnimeTopPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(entries: List<AnimeTopItemEntry>)

    @Query(
        """
        DELETE FROM anime_top_pages
        WHERE type = :type AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deletePage(
        type: String,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM anime_top_items
        WHERE type = :type AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteItems(
        type: String,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM anime_top_items
        WHERE EXISTS (
            SELECT 1 FROM anime_top_pages AS page
            WHERE page.type = anime_top_items.type
                AND page.language = anime_top_items.language
                AND page.`limit` = anime_top_items.`limit`
                AND page.`offset` = anime_top_items.`offset`
                AND page.cachedAt < :minCachedAt
        )
        """
    )
    abstract suspend fun deleteItemsCachedBefore(minCachedAt: Long)

    @Query("DELETE FROM anime_top_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deletePagesCachedBefore(minCachedAt: Long)

    @Transaction
    open suspend fun getPage(
        type: String,
        language: String,
        limit: Int,
        offset: Int,
    ): AnimeTopPageCache? {
        val entry = getPageEntry(type, language, limit, offset) ?: return null
        return AnimeTopPageCache(
            entry = entry,
            items = getItems(type, language, limit, offset),
        )
    }

    @Transaction
    open suspend fun replacePage(
        cache: AnimeTopPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        val entry = cache.entry
        deletePage(entry.type, entry.language, entry.limit, entry.offset)
        deleteItems(entry.type, entry.language, entry.limit, entry.offset)

        insertPage(entry)
        if (cache.items.isNotEmpty()) insertItems(cache.items)

        prunePagesCachedBefore?.let {
            deleteItemsCachedBefore(it)
            deletePagesCachedBefore(it)
        }
    }
}
