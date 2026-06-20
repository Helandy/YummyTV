package su.afk.yummy.tv.feature.comments

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.usecase.AddAnimeCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.DeleteCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.GetAnimeCommentsUseCase
import su.afk.yummy.tv.domain.comments.usecase.GetCommentChildrenUseCase
import su.afk.yummy.tv.domain.comments.usecase.RemoveCommentVoteUseCase
import su.afk.yummy.tv.domain.comments.usecase.ReportCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.UpdateCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.VoteCommentUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.comments.CommentsState.CommentUi
import su.afk.yummy.tv.feature.comments.CommentsState.ComposerMode
import su.afk.yummy.tv.feature.comments.presentation.R
import su.afk.yummy.tv.feature.comments.utils.findUi
import su.afk.yummy.tv.feature.comments.utils.replaceComment
import su.afk.yummy.tv.feature.comments.utils.updateVote

private const val COMMENTS_PAGE_SIZE = 20

@HiltViewModel(assistedFactory = CommentsViewModel.Factory::class)
class CommentsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val settingsStore: SettingsStore,
    private val stringProvider: StringProvider,
    private val getAnimeComments: GetAnimeCommentsUseCase,
    private val getCommentChildren: GetCommentChildrenUseCase,
    private val addAnimeComment: AddAnimeCommentUseCase,
    private val updateComment: UpdateCommentUseCase,
    private val deleteComment: DeleteCommentUseCase,
    private val voteComment: VoteCommentUseCase,
    private val removeCommentVote: RemoveCommentVoteUseCase,
    private val reportComment: ReportCommentUseCase,
    private val analytics: CommentsAnalytics,
) : BaseViewModelNew<CommentsState.State, CommentsState.Event, CommentsState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): CommentsViewModel
    }

    override fun createInitialState() =
        CommentsState.State(comments = createCommentsFlow(CommentSort.BEST))

    private var visibleComments: Map<Int, CommentUi> = emptyMap()

    init {
        analytics.eventScreenOpened(animeId, currentState.sort)
        settingsStore.yaniUserId
            .onEach { userId -> setState { copy(currentUserId = userId) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: CommentsState.Event) {
        when (event) {
            CommentsState.Event.BackSelected -> nav.back()
            CommentsState.Event.RetrySelected -> {
                analytics.eventRetrySelected(animeId, currentState.sort)
                reloadComments()
            }

            CommentsState.Event.RefreshSelected -> {
                analytics.eventRefreshSelected(animeId, currentState.sort)
                reloadComments(forceRefresh = true)
            }

            is CommentsState.Event.SortSelected -> {
                if (event.sort != currentState.sort) {
                    analytics.eventSortSelected(animeId, event.sort)
                    setState {
                        copy(
                            sort = event.sort,
                            comments = createCommentsFlow(event.sort),
                            prependedComments = emptyList(),
                            commentOverlays = emptyMap(),
                            deletedCommentIds = emptySet(),
                            error = null,
                            isModerator = false,
                        )
                    }
                }
            }

            is CommentsState.Event.VisibleCommentsChanged -> {
                visibleComments = event.comments.associateBy { it.comment.id }
            }

            is CommentsState.Event.ComposerTextChanged -> setState { copy(composerText = event.text) }
            CommentsState.Event.SubmitSelected -> submit()
            CommentsState.Event.ComposerCancelled -> resetComposer()
            is CommentsState.Event.ReplySelected -> startReply(event.commentId)
            is CommentsState.Event.EditSelected -> startEdit(event.commentId)
            is CommentsState.Event.DeleteSelected ->
                findComment(event.commentId)?.let {
                    analytics.eventDeleteSelected(animeId, event.commentId)
                    setState { copy(pendingDelete = it) }
                }

            CommentsState.Event.DeleteConfirmed -> confirmDelete()
            CommentsState.Event.DeleteDismissed -> setState { copy(pendingDelete = null) }
            is CommentsState.Event.ReportSelected -> {
                if (!canMutate()) return
                findComment(event.commentId)?.let {
                    analytics.eventReportSelected(animeId, event.commentId)
                    setState { copy(pendingReport = it) }
                }
            }

            is CommentsState.Event.ReportConfirmed -> confirmReport(event.reason)
            CommentsState.Event.ReportDismissed -> setState { copy(pendingReport = null) }
            is CommentsState.Event.VoteSelected -> vote(event.commentId, event.vote)
            is CommentsState.Event.ChildrenToggleSelected -> toggleChildren(event.commentId)
            is CommentsState.Event.LoadMoreChildrenSelected -> loadChildren(
                event.commentId,
                append = true
            )

            is CommentsState.Event.AuthorSelected -> {
                if (event.userId > 0) {
                    analytics.eventAuthorSelected(animeId, event.userId)
                    nav.navigate(accountNavigator.getUserProfileDest(event.userId))
                }
            }
        }
    }

    override fun onRetry() {
        analytics.eventRetrySelected(animeId, currentState.sort)
        reloadComments()
    }

    private fun reloadComments(forceRefresh: Boolean = false) {
        visibleComments = emptyMap()
        setState {
            copy(
                comments = createCommentsFlow(sort, forceRefreshFirstPage = forceRefresh),
                prependedComments = emptyList(),
                commentOverlays = emptyMap(),
                deletedCommentIds = emptySet(),
                error = null,
                isModerator = false,
            )
        }
    }

    private fun createCommentsFlow(
        sort: CommentSort,
        forceRefreshFirstPage: Boolean = false,
    ) = Pager(
        config = PagingConfig(
            pageSize = COMMENTS_PAGE_SIZE,
            initialLoadSize = COMMENTS_PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            OffsetPagingSource { limit, offset ->
                loadCommentsPage(
                    sort = sort,
                    limit = limit,
                    skip = offset,
                    forceRefresh = forceRefreshFirstPage && offset == 0,
                )
            }
        },
    ).flow.cachedIn(viewModelScope)

    private suspend fun loadCommentsPage(
        sort: CommentSort,
        limit: Int,
        skip: Int,
        forceRefresh: Boolean,
    ): OffsetPage<CommentUi> =
        runCatching {
            getAnimeComments(
                animeId = animeId,
                limit = limit,
                skip = skip,
                sort = sort,
                forceRefresh = forceRefresh,
            )
        }.fold(
            onSuccess = { page ->
                setState { copy(isModerator = isModerator || page.isModerator, error = null) }
                OffsetPage(
                    items = page.comments.map { CommentUi(it) },
                    nextOffset = skip + page.comments.size,
                    canLoadMore = page.comments.size >= limit,
                )
            },
            onFailure = { error ->
                analytics.eventLoadError(animeId, sort, error)
                setState {
                    copy(
                        error = error.message ?: stringProvider.get(R.string.comments_load_error),
                    )
                }
                throw error
            },
        )

    private fun submit() {
        if (!canMutate()) return
        val text = currentState.composerText.trim()
        if (text.isBlank()) {
            showToast(R.string.comments_empty_text)
            return
        }
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            when (val mode = currentState.composerMode) {
                ComposerMode.New -> runCatching {
                    addAnimeComment(animeId, CommentDraft(text))
                }.onSuccess { created ->
                    analytics.eventCreated(animeId, created.id)
                    setState {
                        copy(
                            isMutating = false,
                            composerText = "",
                            composerMode = ComposerMode.New,
                            prependedComments = if (sort == CommentSort.OLD) {
                                prependedComments
                            } else {
                                listOf(CommentUi(created)) + prependedComments
                            },
                        )
                    }
                    if (currentState.sort == CommentSort.OLD) reloadComments(forceRefresh = true)
                }.onFailure { showMutationError(it) }

                is ComposerMode.Reply -> runCatching {
                    addAnimeComment(
                        animeId,
                        CommentDraft(
                            text = text,
                            parentCommentId = mode.parentCommentId,
                            replyToCommentId = mode.replyToCommentId,
                        ),
                    )
                }.onSuccess { created ->
                    analytics.eventReplyCreated(animeId, created.id)
                    resetComposer(isMutating = false)
                    loadChildren(mode.parentCommentId, append = false, forceVisible = true)
                }.onFailure { showMutationError(it) }

                is ComposerMode.Edit -> runCatching {
                    updateComment(mode.commentId, text)
                }.onSuccess { updated ->
                    analytics.eventUpdated(animeId, updated.id)
                    setState {
                        copy(
                            isMutating = false,
                            composerText = "",
                            composerMode = ComposerMode.New,
                            commentOverlays = commentOverlays + (
                                    updated.id to (
                                            visibleCommentTree()
                                                .replaceComment(updated)
                                                .findUi(updated.id)
                                                ?: CommentUi(updated)
                                            )
                                    ),
                        )
                    }
                }.onFailure { showMutationError(it) }
            }
        }
    }

    private fun startReply(commentId: Int) {
        if (!canMutate()) return
        val comment = findComment(commentId) ?: return
        val parentId = comment.parentId ?: comment.id
        analytics.eventReplySelected(animeId, commentId)
        setState {
            copy(
                composerMode = ComposerMode.Reply(
                    parentCommentId = parentId,
                    replyToCommentId = comment.id,
                    replyToName = comment.author.name,
                    replyToAvatarUrl = comment.author.avatarSmallUrl,
                ),
                composerText = "",
            )
        }
    }

    private fun startEdit(commentId: Int) {
        val comment = findComment(commentId) ?: return
        if (!canEditOrDelete(comment)) return
        analytics.eventEditSelected(animeId, commentId)
        setState {
            copy(
                composerMode = ComposerMode.Edit(comment.id),
                composerText = comment.text,
            )
        }
    }

    private fun confirmDelete() {
        val comment = currentState.pendingDelete ?: return
        if (!canEditOrDelete(comment)) return
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { deleteComment(comment.id) }.fold(
                onSuccess = { deleted ->
                    if (deleted) {
                        analytics.eventDeleted(animeId, comment.id)
                        setState {
                            copy(
                                isMutating = false,
                                pendingDelete = null,
                                deletedCommentIds = deletedCommentIds + comment.id,
                                prependedComments = prependedComments.filterNot {
                                    it.comment.id == comment.id
                                },
                            )
                        }
                    } else {
                        showMutationError(R.string.comments_delete_error, keepDialog = true)
                    }
                },
                onFailure = {
                    showMutationError(
                        R.string.comments_delete_error,
                        keepDialog = true
                    )
                },
            )
        }
    }

    private fun confirmReport(reason: CommentReportReason) {
        val comment = currentState.pendingReport ?: return
        if (!canMutate()) return
        viewModelScope.launch {
            setState { copy(isMutating = true) }
            runCatching { reportComment(comment.id, reason) }.fold(
                onSuccess = { reported ->
                    if (reported) {
                        analytics.eventReported(animeId, comment.id, reason)
                        setState { copy(isMutating = false, pendingReport = null) }
                        showToast(R.string.comments_report_sent)
                    } else {
                        showMutationError(R.string.comments_report_error, keepDialog = true)
                    }
                },
                onFailure = {
                    showMutationError(
                        R.string.comments_report_error,
                        keepDialog = true
                    )
                },
            )
        }
    }

    private fun vote(commentId: Int, vote: CommentVote) {
        if (!canMutate()) return
        if (vote == CommentVote.NEUTRAL) return
        val comment = findComment(commentId) ?: return
        viewModelScope.launch {
            val result = runCatching {
                if (comment.vote == vote) {
                    removeCommentVote(commentId) to CommentVote.NEUTRAL
                } else {
                    voteComment(commentId, vote) to vote
                }
            }
            result.fold(
                onSuccess = { (voteResult, newVote) ->
                    if (voteResult.success) {
                        analytics.eventVoteChanged(animeId, commentId, newVote)
                        setState {
                            val updated = visibleCommentTree()
                                .updateVote(commentId, voteResult, newVote)
                                .findUi(commentId)
                            if (updated == null) {
                                this
                            } else {
                                copy(commentOverlays = commentOverlays + (commentId to updated))
                            }
                        }
                    } else {
                        showMutationError(R.string.comments_vote_error)
                    }
                },
                onFailure = { showMutationError(R.string.comments_vote_error) },
            )
        }
    }

    private fun toggleChildren(commentId: Int) {
        val item = findCommentUi(commentId) ?: return
        if (item.childrenVisible) {
            analytics.eventRepliesHidden(animeId, commentId)
            updateCommentOverlay(commentId) { copy(childrenVisible = false) }
            return
        }
        if (item.children.isNotEmpty()) {
            analytics.eventRepliesShown(animeId, commentId)
            updateCommentOverlay(commentId) { copy(childrenVisible = true) }
            return
        }
        analytics.eventRepliesShown(animeId, commentId)
        loadChildren(commentId, append = false, forceVisible = true)
    }

    private fun loadChildren(
        commentId: Int,
        append: Boolean,
        forceVisible: Boolean = false,
    ) {
        val item = findCommentUi(commentId) ?: return
        if (item.childrenLoading) return
        viewModelScope.launch {
            val skip = if (append) item.children.size else 0
            if (append) {
                analytics.eventRepliesLoadMoreSelected(animeId, commentId)
            }
            setState {
                val updated = item.copy(
                    childrenLoading = true,
                    childrenError = null,
                    childrenVisible = forceVisible || item.childrenVisible,
                )
                copy(commentOverlays = commentOverlays + (commentId to updated))
            }
            runCatching { getCommentChildren(commentId, skip) }.fold(
                onSuccess = { page ->
                    setState {
                        val current = findCommentUi(commentId) ?: item
                        val updated = current.copy(
                            children = if (append) {
                                current.children + page.comments.map { CommentUi(it) }
                            } else {
                                page.comments.map { CommentUi(it) }
                            },
                            childrenVisible = true,
                            childrenLoading = false,
                            childrenError = null,
                            childrenHasMore = page.comments.size >= COMMENTS_PAGE_SIZE,
                        )
                        copy(
                            commentOverlays = commentOverlays + (commentId to updated),
                            isModerator = page.isModerator || isModerator,
                        )
                    }
                },
                onFailure = { error ->
                    analytics.eventRepliesLoadError(animeId, commentId, error)
                    setState {
                        val current = findCommentUi(commentId) ?: item
                        copy(
                            commentOverlays = commentOverlays + (
                                    commentId to current.copy(
                                        childrenLoading = false,
                                        childrenError = error.message
                                            ?: stringProvider.get(R.string.comments_load_error),
                                    )
                                    )
                        )
                    }
                },
            )
        }
    }

    private fun canMutate(): Boolean {
        if (currentState.isSignedIn) return true
        showToast(R.string.comments_auth_required)
        return false
    }

    private fun canEditOrDelete(comment: Comment): Boolean {
        if (currentState.currentUserId == comment.author.id || currentState.isModerator) return true
        showToast(R.string.comments_permission_denied)
        return false
    }

    private fun resetComposer(isMutating: Boolean = currentState.isMutating) {
        setState {
            copy(
                composerText = "",
                composerMode = ComposerMode.New,
                isMutating = isMutating,
            )
        }
    }

    private fun findComment(commentId: Int): Comment? =
        findCommentUi(commentId)?.comment

    private fun findCommentUi(commentId: Int): CommentUi? =
        visibleCommentTree().findUi(commentId)

    private fun visibleCommentTree(): List<CommentUi> =
        (currentState.prependedComments + visibleComments.values)
            .filterNot { it.comment.id in currentState.deletedCommentIds }
            .map { it.withCurrentOverlays() }

    private fun CommentUi.withCurrentOverlays(): CommentUi {
        val overlaid = currentState.commentOverlays[comment.id] ?: this
        return overlaid.copy(
            children = overlaid.children
                .filterNot { it.comment.id in currentState.deletedCommentIds }
                .map { it.withCurrentOverlays() },
        )
    }

    private fun updateCommentOverlay(
        commentId: Int,
        update: CommentUi.() -> CommentUi,
    ) {
        val item = findCommentUi(commentId) ?: return
        val updated = item.update()
        setState { copy(commentOverlays = commentOverlays + (commentId to updated)) }
    }

    private fun showMutationError(error: Throwable, keepDialog: Boolean = false) {
        val message = error.message ?: stringProvider.get(R.string.comments_action_error)
        setEffect(CommentsState.Effect.ShowToast(message))
        setState {
            copy(
                isMutating = false,
                pendingDelete = if (keepDialog) pendingDelete else null,
                pendingReport = if (keepDialog) pendingReport else null,
            )
        }
    }

    private fun showMutationError(resId: Int, keepDialog: Boolean = false) {
        setEffect(CommentsState.Effect.ShowToast(stringProvider.get(resId)))
        setState {
            copy(
                isMutating = false,
                pendingDelete = if (keepDialog) pendingDelete else null,
                pendingReport = if (keepDialog) pendingReport else null,
            )
        }
    }

    private fun showToast(resId: Int) {
        setEffect(CommentsState.Effect.ShowToast(stringProvider.get(resId)))
    }
}
