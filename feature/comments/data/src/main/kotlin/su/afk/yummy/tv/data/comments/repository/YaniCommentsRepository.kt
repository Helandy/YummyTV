package su.afk.yummy.tv.data.comments.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.storage.comments.CommentsStorageStore
import su.afk.yummy.tv.core.storage.comments.isFresh
import su.afk.yummy.tv.data.comments.dto.YaniClaimCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniPatchCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniPostCommentBodyDto
import su.afk.yummy.tv.data.comments.dto.YaniVoteCommentBodyDto
import su.afk.yummy.tv.data.comments.mapper.toComment
import su.afk.yummy.tv.data.comments.mapper.toCommentItemEntry
import su.afk.yummy.tv.data.comments.mapper.toCommentVoteResult
import su.afk.yummy.tv.data.comments.mapper.toCommentsPage
import su.afk.yummy.tv.data.comments.mapper.toCommentsPageCache
import su.afk.yummy.tv.data.comments.network.YaniCommentsApi
import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentsPage
import su.afk.yummy.tv.domain.comments.repository.CommentsRepository

private const val COMMENTS_CACHE_TTL_MS = 5 * 60 * 1000L
private const val COMMENT_SCOPE_ANIME = "anime"
private const val COMMENT_SCOPE_CHILDREN = "children"
private const val COMMENT_CHILDREN_SORT = "children"
private const val COMMENT_CHILDREN_LIMIT = 20
private const val COMMENT_CACHE_PRUNE_AGE_MS = 24 * 60 * 60 * 1000L

class YaniCommentsRepository(
    private val api: YaniCommentsApi,
    private val commentsStorage: CommentsStorageStore,
) : CommentsRepository {

    override suspend fun getAnimeComments(
        animeId: Int,
        limit: Int,
        skip: Int,
        sort: CommentSort,
        forceRefresh: Boolean,
    ) = withContext(Dispatchers.IO) {
        val stored = commentsStorage.getPage(
            scopeType = COMMENT_SCOPE_ANIME,
            ownerId = animeId,
            sort = sort.apiValue,
            limit = limit,
            skip = skip,
        )
        if (!forceRefresh && stored?.isFresh(COMMENTS_CACHE_TTL_MS) == true) {
            return@withContext stored.toCommentsPage()
        }

        try {
            fetchAnimeCommentsFromNetwork(animeId, limit, skip, sort)
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
        val stored = commentsStorage.getPage(
            scopeType = COMMENT_SCOPE_CHILDREN,
            ownerId = commentId,
            sort = COMMENT_CHILDREN_SORT,
            limit = COMMENT_CHILDREN_LIMIT,
            skip = skip,
        )
        if (stored?.isFresh(COMMENTS_CACHE_TTL_MS) == true) {
            return@withContext stored.toCommentsPage()
        }

        try {
            fetchCommentChildrenFromNetwork(commentId, skip)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toCommentsPage() ?: throw error
        }
    }

    override suspend fun addAnimeComment(
        animeId: Int,
        draft: CommentDraft,
    ) = withContext(Dispatchers.IO) {
        val parentCommentId = draft.parentCommentId
        val comment = api.addAnimeComment(
            animeId = animeId,
            body = YaniPostCommentBodyDto(
                text = draft.text,
                parentComment = parentCommentId,
                replyToComment = draft.replyToCommentId,
            ),
        ).response.toComment()
        if (parentCommentId != null) {
            commentsStorage.invalidateScope(COMMENT_SCOPE_CHILDREN, parentCommentId)
        } else {
            commentsStorage.invalidateScope(COMMENT_SCOPE_ANIME, animeId)
        }
        comment
    }

    override suspend fun updateComment(
        commentId: Int,
        text: String,
    ) = withContext(Dispatchers.IO) {
        api.updateComment(commentId, YaniPatchCommentBodyDto(text))
            .response
            .toComment()
            .also { comment ->
                commentsStorage.updateComment(comment.toDetachedCacheEntry())
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
                    commentsStorage.updateVote(
                        commentId,
                        result.likes,
                        result.dislikes,
                        vote.apiValue
                    )
                }
            }
    }

    override suspend fun removeCommentVote(commentId: Int) =
        withContext(Dispatchers.IO) {
            api.removeCommentVote(commentId).response.toCommentVoteResult()
                .also { result ->
                    if (result.success) {
                        commentsStorage.updateVote(
                            commentId,
                            result.likes,
                            result.dislikes,
                            CommentVote.NEUTRAL.apiValue,
                        )
                    }
                }
        }

    override suspend fun reportComment(
        commentId: Int,
        reason: CommentReportReason,
    ) = withContext(Dispatchers.IO) {
        api.reportComment(commentId, YaniClaimCommentBodyDto(reason.apiValue)).response
    }

    private suspend fun fetchAnimeCommentsFromNetwork(
        animeId: Int,
        limit: Int,
        skip: Int,
        sort: CommentSort,
    ): CommentsPage {
        val page = api.getAnimeComments(animeId, limit, skip, sort.apiValue).toCommentsPage()
        savePage(
            page = page,
            scopeType = COMMENT_SCOPE_ANIME,
            ownerId = animeId,
            sort = sort.apiValue,
            limit = limit,
            skip = skip,
        )
        return page
    }

    private suspend fun fetchCommentChildrenFromNetwork(
        commentId: Int,
        skip: Int,
    ): CommentsPage {
        val page = api.getCommentChildren(commentId, skip).toCommentsPage()
        savePage(
            page = page,
            scopeType = COMMENT_SCOPE_CHILDREN,
            ownerId = commentId,
            sort = COMMENT_CHILDREN_SORT,
            limit = COMMENT_CHILDREN_LIMIT,
            skip = skip,
        )
        return page
    }

    private suspend fun savePage(
        page: CommentsPage,
        scopeType: String,
        ownerId: Int,
        sort: String,
        limit: Int,
        skip: Int,
    ) {
        val cachedAt = System.currentTimeMillis()
        commentsStorage.savePage(
            cache = page.toCommentsPageCache(
                scopeType = scopeType,
                ownerId = ownerId,
                sort = sort,
                limit = limit,
                skip = skip,
                cachedAt = cachedAt,
            ),
            prunePagesCachedBefore = cachedAt - COMMENT_CACHE_PRUNE_AGE_MS,
        )
    }

    private fun su.afk.yummy.tv.domain.comments.model.Comment.toDetachedCacheEntry() =
        toCommentItemEntry(
            scopeType = "",
            ownerId = 0,
            sort = "",
            limit = 0,
            skip = 0,
            position = 0,
        )
}
