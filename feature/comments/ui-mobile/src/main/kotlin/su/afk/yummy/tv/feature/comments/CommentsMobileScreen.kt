package su.afk.yummy.tv.feature.comments

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.comments.mobile.R
import su.afk.yummy.tv.feature.comments.utils.uiMessage
import su.afk.yummy.tv.feature.comments.view.CommentSortRow
import su.afk.yummy.tv.feature.comments.view.CommentsComposer
import su.afk.yummy.tv.feature.comments.view.CommentsDialogs
import su.afk.yummy.tv.feature.comments.view.CommentsList
import su.afk.yummy.tv.feature.comments.view.resolve

@Preview(
    name = "Default",
    device = "spec:width=412dp,height=915dp,dpi=420",
    showBackground = true
)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CommentsMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        CommentsMobileScreen(CommentsState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CommentsMobileScreen(
    state: CommentsState.State,
    effect: Flow<CommentsState.Effect>,
    onEvent: (CommentsState.Event) -> Unit,
) {
    val context = LocalContext.current
    val pagingComments = state.comments.collectAsLazyPagingItems()
    val refreshState = pagingComments.loadState.refresh
    val appendState = pagingComments.loadState.append
    val visibleComments = remember(
        state.prependedComments,
        state.commentOverlays,
        state.deletedCommentIds,
        pagingComments.itemSnapshotList.items,
    ) {
        (state.prependedComments + pagingComments.itemSnapshotList.items)
            .distinctBy { it.comment.id }
            .mapNotNull { it.resolve(state) }
    }
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { visibleComments.isEmpty() }
        ?.error
        ?.uiMessage()
    val listState = rememberLazyListState()
    var sortRowVisible by remember { mutableStateOf(true) }
    var previousScrollIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is CommentsState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val scrollingDown = index > previousScrollIndex ||
                    (index == previousScrollIndex && offset > previousScrollOffset)
            val scrollingUp = index < previousScrollIndex ||
                    (index == previousScrollIndex && offset < previousScrollOffset)
            sortRowVisible = when {
                index == 0 && offset == 0 -> true
                scrollingUp -> true
                scrollingDown -> false
                else -> sortRowVisible
            }
            previousScrollIndex = index
            previousScrollOffset = offset
        }
    }
    LaunchedEffect(state.sort) {
        sortRowVisible = true
        listState.scrollToItem(0)
    }
    LaunchedEffect(visibleComments) {
        onEvent(CommentsState.Event.VisibleCommentsChanged(visibleComments))
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.comments_title),
                onBack = { onEvent(CommentsState.Event.BackSelected) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = sortRowVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                CommentSortRow(
                    selected = state.sort,
                    onSelected = { onEvent(CommentsState.Event.SortSelected(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Box(Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = refreshState is LoadState.Loading && visibleComments.isNotEmpty(),
                    onRefresh = { onEvent(CommentsState.Event.RefreshSelected) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    MobileStateContent(
                        isLoading = refreshState is LoadState.Loading && visibleComments.isEmpty(),
                        error = initialError,
                        empty = refreshState !is LoadState.Loading &&
                                visibleComments.isEmpty() &&
                                initialError == null,
                        emptyText = stringResource(R.string.comments_empty),
                        onRetry = {
                            onEvent(CommentsState.Event.RetrySelected)
                            pagingComments.retry()
                        },
                    ) {
                        CommentsList(
                            state = state,
                            pagingComments = pagingComments,
                            appendState = appendState,
                            listState = listState,
                            onEvent = onEvent,
                        )
                    }
                }
            }
            CommentsComposer(
                isSignedIn = state.isSignedIn,
                text = state.composerText,
                mode = state.composerMode,
                enabled = !state.isMutating,
                onTextChange = { onEvent(CommentsState.Event.ComposerTextChanged(it)) },
                onSubmit = { onEvent(CommentsState.Event.SubmitSelected) },
                onCancel = { onEvent(CommentsState.Event.ComposerCancelled) },
            )
        }
        CommentsDialogs(
            pendingDelete = state.pendingDelete,
            pendingReport = state.pendingReport,
            isMutating = state.isMutating,
            onDeleteConfirm = { onEvent(CommentsState.Event.DeleteConfirmed) },
            onDeleteDismiss = { onEvent(CommentsState.Event.DeleteDismissed) },
            onReportConfirm = { onEvent(CommentsState.Event.ReportConfirmed(it)) },
            onReportDismiss = { onEvent(CommentsState.Event.ReportDismissed) },
        )
    }
}
