package su.afk.yummy.tv.feature.comments.tv

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.tv.utils.resolve
import su.afk.yummy.tv.feature.comments.tv.utils.uiMessage
import su.afk.yummy.tv.feature.comments.tv.view.CommentsComposer
import su.afk.yummy.tv.feature.comments.tv.view.CommentsDialogs
import su.afk.yummy.tv.feature.comments.tv.view.CommentsHeader
import su.afk.yummy.tv.feature.comments.tv.view.CommentsList

@Composable
fun CommentsTvScreen(
    state: CommentsState.State,
    effect: Flow<CommentsState.Effect>,
    onEvent: (CommentsState.Event) -> Unit,
) {
    val context = LocalContext.current
    val comments = state.comments.collectAsLazyPagingItems()
    val refreshState = comments.loadState.refresh
    val listState = rememberLazyListState()
    val headerFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val composerFocusRequester = remember { FocusRequester() }
    var isComposerEditing by remember { mutableStateOf(false) }
    var dialogReturnFocusRequester by remember { mutableStateOf<FocusRequester?>(null) }

    val visibleComments = remember(
        state.prependedComments,
        state.commentOverlays,
        state.deletedCommentIds,
        comments.itemSnapshotList.items,
    ) {
        (state.prependedComments + comments.itemSnapshotList.items)
            .distinctBy { it.comment.id }
            .mapNotNull { it.resolve(state) }
    }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is CommentsState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { headerFocusRequester.requestFocus() }
    }
    LaunchedEffect(refreshState) {
        if (refreshState is LoadState.Error && visibleComments.isEmpty()) {
            withFrameNanos { }
            runCatching { retryFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(state.sort) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(visibleComments) {
        onEvent(CommentsState.Event.VisibleCommentsChanged(visibleComments))
    }
    LaunchedEffect(state.composerMode) {
        if (state.composerMode !is CommentsState.ComposerMode.New) {
            isComposerEditing = true
            withFrameNanos { }
            runCatching { composerFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(state.pendingDelete, state.pendingReport) {
        if (state.pendingDelete == null && state.pendingReport == null) {
            val requester = dialogReturnFocusRequester ?: return@LaunchedEffect
            dialogReturnFocusRequester = null
            withFrameNanos { }
            val restored = runCatching { requester.requestFocus() }.getOrDefault(false)
            if (!restored) runCatching { headerFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = state.pendingDelete == null && state.pendingReport == null) {
        when {
            isComposerEditing -> isComposerEditing = false
            state.composerMode !is CommentsState.ComposerMode.New ->
                onEvent(CommentsState.Event.ComposerCancelled)

            else -> onEvent(CommentsState.Event.BackSelected)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = TvScreenPadding.Horizontal,
                vertical = TvScreenPadding.Vertical,
            ),
    ) {
        Text(
            text = stringResource(R.string.comments_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        CommentsHeader(
            selectedSort = state.sort,
            initialFocusRequester = headerFocusRequester,
            onSortSelected = { onEvent(CommentsState.Event.SortSelected(it)) },
            onRefresh = { onEvent(CommentsState.Event.RefreshSelected) },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                refreshState is LoadState.Loading && visibleComments.isEmpty() ->
                    TvLoadingScreen()

                refreshState is LoadState.Error && visibleComments.isEmpty() ->
                    TvStateMessage(
                        title = refreshState.error.uiMessage(),
                        icon = Icons.Filled.Warning,
                        retryLabel = stringResource(R.string.comments_retry),
                        retryFocusRequester = retryFocusRequester,
                        onRetry = {
                            onEvent(CommentsState.Event.RetrySelected)
                            comments.retry()
                        },
                    )

                visibleComments.isEmpty() -> TvStateMessage(
                    title = stringResource(R.string.comments_empty),
                    icon = Icons.Filled.ChatBubbleOutline,
                )

                else -> CommentsList(
                    state = state,
                    comments = comments,
                    listState = listState,
                    fallbackFocusRequester = headerFocusRequester,
                    onReply = { onEvent(CommentsState.Event.ReplySelected(it)) },
                    onEdit = { onEvent(CommentsState.Event.EditSelected(it)) },
                    onDelete = { id, requester ->
                        dialogReturnFocusRequester = requester
                        onEvent(CommentsState.Event.DeleteSelected(id))
                    },
                    onReport = { id, requester ->
                        dialogReturnFocusRequester = requester
                        onEvent(CommentsState.Event.ReportSelected(id))
                    },
                    onVote = { id, vote ->
                        onEvent(CommentsState.Event.VoteSelected(id, vote))
                    },
                    onToggleChildren = {
                        onEvent(CommentsState.Event.ChildrenToggleSelected(it))
                    },
                    onLoadMoreChildren = {
                        onEvent(CommentsState.Event.LoadMoreChildrenSelected(it))
                    },
                    onRetry = comments::retry,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        CommentsComposer(
            isSignedIn = state.isSignedIn,
            text = state.composerText,
            mode = state.composerMode,
            enabled = !state.isMutating,
            isEditing = isComposerEditing,
            focusRequester = composerFocusRequester,
            onEditingChanged = { isComposerEditing = it },
            onTextChanged = { onEvent(CommentsState.Event.ComposerTextChanged(it)) },
            onSubmit = { onEvent(CommentsState.Event.SubmitSelected) },
            onCancel = {
                isComposerEditing = false
                onEvent(CommentsState.Event.ComposerCancelled)
            },
        )
    }
    CommentsDialogs(
        pendingDelete = state.pendingDelete,
        pendingReport = state.pendingReport,
        isMutating = state.isMutating,
        onDeleteConfirmed = { onEvent(CommentsState.Event.DeleteConfirmed) },
        onDeleteDismissed = { onEvent(CommentsState.Event.DeleteDismissed) },
        onReportConfirmed = { onEvent(CommentsState.Event.ReportConfirmed(it)) },
        onReportDismissed = { onEvent(CommentsState.Event.ReportDismissed) },
    )
}
