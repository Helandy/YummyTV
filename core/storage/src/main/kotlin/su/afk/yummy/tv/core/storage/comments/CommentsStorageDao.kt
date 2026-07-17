package su.afk.yummy.tv.core.storage.comments

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class CommentsStorageDao {

    @Query(
        """
        SELECT * FROM comment_pages
        WHERE scopeType = :scopeType
            AND ownerId = :ownerId
            AND sort = :sort
            AND `limit` = :limit
            AND skip = :skip
        LIMIT 1
        """
    )
    abstract suspend fun getPageEntry(
        scopeType: String,
        ownerId: Int,
        sort: String,
        limit: Int,
        skip: Int,
    ): CommentPageEntry?

    @Query(
        """
        SELECT * FROM comment_items
        WHERE scopeType = :scopeType
            AND ownerId = :ownerId
            AND sort = :sort
            AND `limit` = :limit
            AND skip = :skip
        ORDER BY position
        """
    )
    abstract suspend fun getItems(
        scopeType: String,
        ownerId: Int,
        sort: String,
        limit: Int,
        skip: Int,
    ): List<CommentItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPage(entry: CommentPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(entries: List<CommentItemEntry>)

    @Query(
        """
        DELETE FROM comment_pages
        WHERE scopeType = :scopeType
            AND ownerId = :ownerId
            AND sort = :sort
            AND `limit` = :limit
            AND skip = :skip
        """
    )
    abstract suspend fun deletePage(
        scopeType: String,
        ownerId: Int,
        sort: String,
        limit: Int,
        skip: Int,
    )

    @Query(
        """
        DELETE FROM comment_items
        WHERE scopeType = :scopeType
            AND ownerId = :ownerId
            AND sort = :sort
            AND `limit` = :limit
            AND skip = :skip
        """
    )
    abstract suspend fun deleteItems(
        scopeType: String,
        ownerId: Int,
        sort: String,
        limit: Int,
        skip: Int,
    )

    @Query("DELETE FROM comment_pages WHERE scopeType = :scopeType AND ownerId = :ownerId")
    abstract suspend fun deletePagesForScope(scopeType: String, ownerId: Int)

    @Query("DELETE FROM comment_items WHERE scopeType = :scopeType AND ownerId = :ownerId")
    abstract suspend fun deleteItemsForScope(scopeType: String, ownerId: Int)

    @Query("DELETE FROM comment_pages WHERE scopeType LIKE :scopePrefix || '%' AND ownerId = :ownerId")
    abstract suspend fun deletePagesForScopePrefix(scopePrefix: String, ownerId: Int)

    @Query("DELETE FROM comment_items WHERE scopeType LIKE :scopePrefix || '%' AND ownerId = :ownerId")
    abstract suspend fun deleteItemsForScopePrefix(scopePrefix: String, ownerId: Int)

    @Query("DELETE FROM comment_items WHERE commentId = :commentId")
    abstract suspend fun deleteItemsByCommentId(commentId: Int)

    @Query(
        """
        SELECT DISTINCT page.* FROM comment_pages AS page
        INNER JOIN comment_items AS item
            ON item.scopeType = page.scopeType
            AND item.ownerId = page.ownerId
            AND item.sort = page.sort
            AND item.`limit` = page.`limit`
            AND item.skip = page.skip
        WHERE item.commentId = :commentId
        """
    )
    abstract suspend fun getPagesContainingComment(commentId: Int): List<CommentPageEntry>

    @Query(
        """
        SELECT DISTINCT parentId FROM comment_items
        WHERE commentId = :commentId AND parentId IS NOT NULL
        """
    )
    abstract suspend fun getParentIds(commentId: Int): List<Int>

    @Query(
        """
        UPDATE comment_items
        SET text = :text,
            authorName = :authorName,
            avatarSmallUrl = :avatarSmallUrl,
            avatarBigUrl = :avatarBigUrl,
            avatarFullUrl = :avatarFullUrl,
            childrenCount = :childrenCount,
            likes = :likes,
            dislikes = :dislikes,
            vote = :vote,
            roles = :roles,
            deletedAtEpochSeconds = :deletedAtEpochSeconds
        WHERE commentId = :commentId
        """
    )
    abstract suspend fun updateComment(
        commentId: Int,
        text: String,
        authorName: String,
        avatarSmallUrl: String?,
        avatarBigUrl: String?,
        avatarFullUrl: String?,
        childrenCount: Int,
        likes: Int,
        dislikes: Int,
        vote: Int,
        roles: String,
        deletedAtEpochSeconds: Long?,
    )

    @Query(
        """
        UPDATE comment_items
        SET likes = :likes,
            dislikes = :dislikes,
            vote = :vote
        WHERE commentId = :commentId
        """
    )
    abstract suspend fun updateVote(commentId: Int, likes: Int, dislikes: Int, vote: Int)

    @Query(
        """
        DELETE FROM comment_items
        WHERE EXISTS (
            SELECT 1 FROM comment_pages AS page
            WHERE page.scopeType = comment_items.scopeType
                AND page.ownerId = comment_items.ownerId
                AND page.sort = comment_items.sort
                AND page.`limit` = comment_items.`limit`
                AND page.skip = comment_items.skip
                AND page.cachedAt < :minCachedAt
        )
        """
    )
    abstract suspend fun deleteItemsCachedBefore(minCachedAt: Long)

    @Query("DELETE FROM comment_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deletePagesCachedBefore(minCachedAt: Long)

    @Transaction
    open suspend fun getPage(
        scopeType: String,
        ownerId: Int,
        sort: String,
        limit: Int,
        skip: Int,
    ): CommentsPageCache? {
        val entry = getPageEntry(scopeType, ownerId, sort, limit, skip) ?: return null
        return CommentsPageCache(
            entry = entry,
            items = getItems(scopeType, ownerId, sort, limit, skip)
        )
    }

    @Transaction
    open suspend fun replacePage(
        cache: CommentsPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        val entry = cache.entry
        deletePage(entry.scopeType, entry.ownerId, entry.sort, entry.limit, entry.skip)
        deleteItems(entry.scopeType, entry.ownerId, entry.sort, entry.limit, entry.skip)

        insertPage(entry)
        if (cache.items.isNotEmpty()) insertItems(cache.items)

        prunePagesCachedBefore?.let {
            deleteItemsCachedBefore(it)
            deletePagesCachedBefore(it)
        }
    }

    @Transaction
    open suspend fun invalidateScope(scopeType: String, ownerId: Int) {
        deleteItemsForScope(scopeType, ownerId)
        deletePagesForScope(scopeType, ownerId)
    }

    @Transaction
    open suspend fun invalidateScopePrefix(scopePrefix: String, ownerId: Int) {
        deleteItemsForScopePrefix(scopePrefix, ownerId)
        deletePagesForScopePrefix(scopePrefix, ownerId)
    }

    @Transaction
    open suspend fun invalidatePagesContainingComment(commentId: Int) {
        val affectedCommentIds = listOf(commentId) + getParentIds(commentId)
        affectedCommentIds
            .flatMap { id -> getPagesContainingComment(id) }
            .distinct()
            .forEach { page ->
                deleteItems(page.scopeType, page.ownerId, page.sort, page.limit, page.skip)
                deletePage(page.scopeType, page.ownerId, page.sort, page.limit, page.skip)
            }
    }
}
