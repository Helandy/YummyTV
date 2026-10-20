package su.afk.yummy.tv.feature.comments.mobile

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.error.uiMessage
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileStateContent
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.mobile.view.CommentSortOverlay
import su.afk.yummy.tv.feature.comments.mobile.view.CommentSortRowHeight
import su.afk.yummy.tv.feature.comments.mobile.view.CommentsComposer
import su.afk.yummy.tv.feature.comments.mobile.view.CommentsDialogs
import su.afk.yummy.tv.feature.comments.mobile.view.CommentsList
import su.afk.yummy.tv.feature.comments.mobile.view.commentSortOrder
import su.afk.yummy.tv.feature.comments.presentation.R
import su.afk.yummy.tv.feature.comments.utils.buildVisibleComments

private const val SORT_ROW_SCROLL_THRESHOLD = 6f

@Preview(
    name = "Default",
    device = "spec:width=412dp,height=915dp,dpi=420",
    showBackground = true,
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
        buildVisibleComments(state, pagingComments.itemSnapshotList.items)
    }
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { visibleComments.isEmpty() }
        ?.error
        ?.uiMessage()
    val listState = rememberLazyListState()
    var sortRowVisible by remember { mutableStateOf(true) }
    val sortRowScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            // Реагируем только на жесты пальца: сдвиги из-за подгрузки картинок/высот не должны показывать шапку.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                when {
                    !listState.canScrollBackward -> sortRowVisible = true
                    consumed.y < -SORT_ROW_SCROLL_THRESHOLD -> sortRowVisible = false
                    consumed.y > SORT_ROW_SCROLL_THRESHOLD -> sortRowVisible = true
                }
                return Offset.Zero
            }
        }
    }
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = commentSortOrder.indexOf(state.sort).coerceAtLeast(0),
        pageCount = commentSortOrder.size,
        onPageSelected = { page ->
            commentSortOrder.getOrNull(page)?.let { onEvent(CommentsState.Event.SortSelected(it)) }
        },
    )

    LaunchedEffect(Unit) {
        effect.collect { event ->
            when (event) {
                is CommentsState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
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
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Box(modifier = Modifier.weight(1f).nestedScroll(sortRowScrollConnection)) {
                MobileSwipeableTabsPager(
                    state = tabsState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> commentSortOrder[page].name },
                ) { page ->
                    if (commentSortOrder.getOrNull(page) == state.sort) {
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
                                    topInset = CommentSortRowHeight,
                                    onEvent = onEvent,
                                )
                            }
                        }
                    } else {
                        MobileSectionLoading(modifier = Modifier.fillMaxSize())
                    }
                }
                CommentSortOverlay(
                    visible = sortRowVisible,
                    selected = state.sort,
                    onSelected = { tabsState.selectPage(commentSortOrder.indexOf(it)) },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
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
