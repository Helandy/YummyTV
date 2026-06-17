package su.afk.yummy.tv.core.storage.comments

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "comment_pages",
    primaryKeys = ["scopeType", "ownerId", "sort", "limit", "skip"],
    indices = [
        Index(value = ["cachedAt"], name = "index_comment_pages_cachedAt"),
        Index(value = ["scopeType", "ownerId"], name = "index_comment_pages_scope"),
    ],
)
data class CommentPageEntry(
    val scopeType: String,
    val ownerId: Int,
    val sort: String,
    val limit: Int,
    val skip: Int,
    val responseSize: Int,
    val isModerator: Boolean,
    val cachedAt: Long,
)

@Entity(
    tableName = "comment_items",
    primaryKeys = ["scopeType", "ownerId", "sort", "limit", "skip", "position"],
    indices = [
        Index(
            value = ["scopeType", "ownerId", "sort", "limit", "skip"],
            name = "index_comment_items_page",
        ),
        Index(value = ["commentId"], name = "index_comment_items_commentId"),
    ],
)
data class CommentItemEntry(
    val scopeType: String,
    val ownerId: Int,
    val sort: String,
    val limit: Int,
    val skip: Int,
    val position: Int,
    val commentId: Int,
    val authorId: Int,
    val authorName: String,
    val avatarSmallUrl: String?,
    val avatarBigUrl: String?,
    val avatarFullUrl: String?,
    val text: String,
    val createdAtEpochSeconds: Long,
    val parentId: Int?,
    val childrenCount: Int,
    val likes: Int,
    val dislikes: Int,
    val vote: Int,
    val roles: String,
    val deletedAtEpochSeconds: Long?,
)
