package su.afk.yummy.tv.data.comments.mapper

import su.afk.yummy.tv.core.storage.comments.CommentItemEntry
import su.afk.yummy.tv.core.storage.comments.CommentPageEntry
import su.afk.yummy.tv.core.storage.comments.CommentsPageCache
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.data.comments.dto.YaniCommentDto
import su.afk.yummy.tv.data.comments.dto.YaniCommentsResponseDto
import su.afk.yummy.tv.data.comments.dto.YaniVoteCommentPayloadDto
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentAuthor
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentVoteResult
import su.afk.yummy.tv.domain.comments.model.CommentsPage

private const val ROLE_SEPARATOR = "|"

internal fun YaniCommentDto.toComment(): Comment =
    Comment(
        id = id,
        author = CommentAuthor(
            id = userId,
            name = name,
            avatarSmallUrl = avatars.small.toHttpsUrlOrNull(),
            avatarBigUrl = avatars.big.toHttpsUrlOrNull(),
            avatarFullUrl = avatars.full.toHttpsUrlOrNull(),
        ),
        text = text,
        createdAtEpochSeconds = time,
        parentId = parentId.takeIf { it > 0 },
        childrenCount = childrenCount.coerceAtLeast(0),
        likes = likes.coerceAtLeast(0),
        dislikes = dislikes.coerceAtLeast(0),
        vote = CommentVote.fromApi(vote),
        roles = roles,
        deletedAtEpochSeconds = deletedAt.takeIf { it > 0 },
    )

internal fun YaniVoteCommentPayloadDto.toCommentVoteResult(): CommentVoteResult =
    CommentVoteResult(
        likes = likes.coerceAtLeast(0),
        dislikes = dislikes.coerceAtLeast(0),
        success = success,
    )

internal fun YaniCommentsResponseDto.toCommentsPageCache(
    scopeType: String,
    ownerId: Int,
    sort: String,
    limit: Int,
    skip: Int,
    cachedAt: Long,
): CommentsPageCache =
    CommentsPageCache(
        entry = CommentPageEntry(
            scopeType = scopeType,
            ownerId = ownerId,
            sort = sort,
            limit = limit,
            skip = skip,
            responseSize = response.comments.size,
            isModerator = response.isModerator,
            cachedAt = cachedAt,
        ),
        items = response.comments.mapIndexed { index, comment ->
            comment.toCommentItemEntry(
                scopeType = scopeType,
                ownerId = ownerId,
                sort = sort,
                limit = limit,
                skip = skip,
                position = index,
            )
        },
    )

internal fun CommentsPageCache.toCommentsPage(): CommentsPage =
    CommentsPage(
        comments = items.map { it.toComment() },
        isModerator = entry.isModerator,
    )

internal fun YaniCommentDto.toCommentItemEntry(
    scopeType: String,
    ownerId: Int,
    sort: String,
    limit: Int,
    skip: Int,
    position: Int,
): CommentItemEntry =
    CommentItemEntry(
        scopeType = scopeType,
        ownerId = ownerId,
        sort = sort,
        limit = limit,
        skip = skip,
        position = position,
        commentId = id,
        authorId = userId,
        authorName = name,
        avatarSmallUrl = avatars.small.toHttpsUrlOrNull(),
        avatarBigUrl = avatars.big.toHttpsUrlOrNull(),
        avatarFullUrl = avatars.full.toHttpsUrlOrNull(),
        text = text,
        createdAtEpochSeconds = time,
        parentId = parentId.takeIf { it > 0 },
        childrenCount = childrenCount.coerceAtLeast(0),
        likes = likes.coerceAtLeast(0),
        dislikes = dislikes.coerceAtLeast(0),
        vote = CommentVote.fromApi(vote).apiValue,
        roles = roles.joinToString(ROLE_SEPARATOR),
        deletedAtEpochSeconds = deletedAt.takeIf { it > 0 },
    )

private fun CommentItemEntry.toComment(): Comment =
    Comment(
        id = commentId,
        author = CommentAuthor(
            id = authorId,
            name = authorName,
            avatarSmallUrl = avatarSmallUrl,
            avatarBigUrl = avatarBigUrl,
            avatarFullUrl = avatarFullUrl,
        ),
        text = text,
        createdAtEpochSeconds = createdAtEpochSeconds,
        parentId = parentId,
        childrenCount = childrenCount,
        likes = likes,
        dislikes = dislikes,
        vote = CommentVote.fromApi(vote),
        roles = roles.split(ROLE_SEPARATOR).filter { it.isNotBlank() },
        deletedAtEpochSeconds = deletedAtEpochSeconds,
    )

private fun String?.toHttpsUrlOrNull(): String? {
    val value = this?.takeIf { it.isNotBlank() } ?: return null
    return value.toHttpsUrl()
}
