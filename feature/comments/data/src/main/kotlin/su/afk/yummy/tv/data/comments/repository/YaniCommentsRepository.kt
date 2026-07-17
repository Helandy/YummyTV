package su.afk.yummy.tv.data.comments.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.comments.CommentsStorageStore
import su.afk.yummy.tv.core.storage.comments.isFresh
import su.afk.yummy.tv.data.comments.dto.YaniClaimCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniPatchCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniPostCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniVoteCommentBodyDto
import su.afk.yummy.tv.data.comments.mapper.toComment
import su.afk.yummy.tv.data.comments.mapper.toCommentVoteResult
import su.afk.yummy.tv.data.comments.mapper.toCommentsPage
import su.afk.yummy.tv.data.comments.mapper.toCommentsPageCache
import su.afk.yummy.tv.data.comments.network.YaniCommentsApi
import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentsPage
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository

private const val COMMENTS_CACHE_TTL_MS = 5 * 60 * 1000L
private const val COMMENT_SCOPE_CHILDREN = "children"
private const val COMMENT_CHILDREN_SORT = "children"
private const val COMMENT_CHILDREN_LIMIT = 20
private const val COMMENT_CACHE_PRUNE_AGE_MS = 24 * 60 * 60 * 1000L

class YaniCommentsRepository(
    private val api: YaniCommentsApi,
    private val commentsStorage: CommentsStorageStore,
    private val settingsStore: SettingsStore,
) : CommentsRepository {

    override suspend fun getComments(
        targetType: CommentTargetType,
        targetId: Int,
        limit: Int,
        skip: Int,
        sort: CommentSort,
        forceRefresh: Boolean,
    ) = withContext(Dispatchers.IO) {
        val scopeType = cacheScope(targetType.apiValue)
        val stored = commentsStorage.getPage(
            scopeType = scopeType,
            ownerId = targetId,
            sort = sort.apiValue,
            limit = limit,
            skip = skip,
        )
        if (!forceRefresh && stored?.isFresh(COMMENTS_CACHE_TTL_MS) == true) {
            return@withContext stored.toCommentsPage()
        }

        try {
            fetchCommentsFromNetwork(targetType, targetId, limit, skip, sort, scopeType)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toCommentsPage() ?: throw error
        }
    }

    override suspend fun getCommentChildren(
        commentId: Int,
        skip: Int,
    ) = withContext(Dispatchers.IO) {
        val scopeType = cacheScope(COMMENT_SCOPE_CHILDREN)
        val stored = commentsStorage.getPage(
            scopeType = scopeType,
            ownerId = commentId,
            sort = COMMENT_CHILDREN_SORT,
            limit = COMMENT_CHILDREN_LIMIT,
            skip = skip,
        )
        if (stored?.isFresh(COMMENTS_CACHE_TTL_MS) == true) {
            return@withContext stored.toCommentsPage()
        }

        try {
            fetchCommentChildrenFromNetwork(commentId, skip, scopeType)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toCommentsPage() ?: throw error
        }
    }

    override suspend fun addComment(
        targetType: CommentTargetType,
        targetId: Int,
        draft: CommentDraft,
    ) = withContext(Dispatchers.IO) {
        val parentCommentId = draft.parentCommentId
        val comment = api.addComment(
            targetType = targetType.apiValue,
            targetId = targetId,
            body = YaniPostCommentBodyDto(
                text = draft.text,
                parentComment = parentCommentId,
                replyToComment = draft.replyToCommentId,
            ),
        ).response.toComment()
        if (parentCommentId != null) {
            commentsStorage.invalidateScopePrefix(
                cacheScopePrefix(COMMENT_SCOPE_CHILDREN),
                parentCommentId,
            )
            commentsStorage.invalidateScopePrefix(cacheScopePrefix(targetType.apiValue), targetId)
        } else {
            commentsStorage.invalidateScopePrefix(cacheScopePrefix(targetType.apiValue), targetId)
        }
        comment
    }

    override suspend fun updateComment(
        commentId: Int,
        text: String,
    ) = withContext(Dispatchers.IO) {
        api.updateComment(commentId, YaniPatchCommentBodyDto(text))
            .response
            .let { dto ->
                commentsStorage.deleteComment(commentId)
                dto.toComment()
            }
    }

    override suspend fun deleteComment(commentId: Int) =
        withContext(Dispatchers.IO) {
            api.deleteComment(commentId).response.also { success ->
                if (success) commentsStorage.deleteComment(commentId)
            }
        }

    override suspend fun voteComment(
        commentId: Int,
        vote: CommentVote,
    ) = withContext(Dispatchers.IO) {
        require(vote != CommentVote.NEUTRAL)
        api.voteComment(commentId, YaniVoteCommentBodyDto(vote.apiValue))
            .response
            .toCommentVoteResult()
            .also { result ->
                if (result.success) {
                    commentsStorage.deleteComment(commentId)
                }
            }
    }

    override suspend fun removeCommentVote(commentId: Int) =
        withContext(Dispatchers.IO) {
            api.removeCommentVote(commentId).response.toCommentVoteResult()
                .also { result ->
                    if (result.success) {
                        commentsStorage.deleteComment(commentId)
                    }
                }
        }

    override suspend fun reportComment(
        commentId: Int,
        reason: CommentReportReason,
    ) = withContext(Dispatchers.IO) {
        api.reportComment(commentId, YaniClaimCommentBodyDto(reason.apiValue)).response
    }

    private suspend fun fetchCommentsFromNetwork(
        targetType: CommentTargetType,
        targetId: Int,
        limit: Int,
        skip: Int,
        sort: CommentSort,
        cacheScopeType: String,
    ): CommentsPage {
        return savePage(
            dto = api.getComments(targetType.apiValue, targetId, limit, skip, sort.apiValue),
            scopeType = cacheScopeType,
            ownerId = targetId,
            sort = sort.apiValue,
            limit = limit,
            skip = skip,
        )
    }

    private suspend fun fetchCommentChildrenFromNetwork(
        commentId: Int,
        skip: Int,
        cacheScopeType: String,
    ): CommentsPage {
        return savePage(
            dto = api.getCommentChildren(commentId, skip),
            scopeType = cacheScopeType,
            ownerId = commentId,
            sort = COMMENT_CHILDREN_SORT,
            limit = COMMENT_CHILDREN_LIMIT,
            skip = skip,
        )
    }

    private suspend fun savePage(
        dto: su.afk.yummy.tv.data.comments.dto.YaniCommentsResponseDto,
        scopeType: String,
        ownerId: Int,
        sort: String,
        limit: Int,
        skip: Int,
    ): CommentsPage {
        val cachedAt = System.currentTimeMillis()
        val cache = dto.toCommentsPageCache(
            scopeType = scopeType,
            ownerId = ownerId,
            sort = sort,
            limit = limit,
            skip = skip,
            cachedAt = cachedAt,
        )
        commentsStorage.savePage(
            cache = cache,
            prunePagesCachedBefore = cachedAt - COMMENT_CACHE_PRUNE_AGE_MS,
        )
        return cache.toCommentsPage()
    }

    private suspend fun cacheScope(scopeType: String): String {
        val userId = settingsStore.yaniUserId.first().coerceAtLeast(0)
        val language = settingsStore.yaniContentLanguage.first().apiCode
        return "${cacheScopePrefix(scopeType)}$userId:$language"
    }

    private fun cacheScopePrefix(scopeType: String): String = "$scopeType:"
}
