package su.afk.yummy.tv.feature.comments.mobile.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.feature.comments.CommentsState

/**
 * Неактивная вкладка сортировки: показывает уже предзагруженный список, пока пейджер листается,
 * чтобы вместо спиннера была настоящая лента. Правки (оверлеи, свои комментарии) относятся только
 * к активной вкладке и сюда не попадают.
 */
@Composable
internal fun CommentsNeighborPage(
    state: CommentsState.State,
    sort: CommentSort,
    topInset: Dp,
    onEvent: (CommentsState.Event) -> Unit,
) {
    val flow = state.commentsBySort[sort]
    if (flow == null) {
        MobileSectionLoading(modifier = Modifier.fillMaxSize())
        return
    }
    val items = flow.collectAsLazyPagingItems()
    val neighborState = remember(state) {
        state.copy(
            prependedComments = persistentListOf(),
            commentOverlays = persistentMapOf(),
            deletedCommentIds = persistentSetOf(),
        )
    }
    if (items.itemCount == 0 && items.loadState.refresh is LoadState.Loading) {
        MobileSectionLoading(modifier = Modifier.fillMaxSize())
        return
    }
    CommentsList(
        state = neighborState,
        pagingComments = items,
        appendState = items.loadState.append,
        listState = rememberLazyListState(),
        topInset = topInset,
        onEvent = onEvent,
    )
}
