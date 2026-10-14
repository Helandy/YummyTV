package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

fun <Key : Any> launchTvLazyListKeyFocusRestore(
    previousJob: Job?,
    scope: CoroutineScope,
    restoreState: TvLazyFocusRestoreState<Key>,
    keys: List<Key>,
    listState: LazyListState,
    itemFocusRequesters: Map<Key, FocusRequester>,
    fallbackFocusRequester: FocusRequester,
    fallbackIndex: Int = 0,
    lazyIndexOffset: Int = 0,
    onRestoreFinished: () -> Unit = {},
): Job = launchTvLazyKeyFocusRestore(
    previousJob = previousJob,
    scope = scope,
    restoreState = restoreState,
    keys = keys,
    itemFocusRequesters = itemFocusRequesters,
    fallbackFocusRequester = fallbackFocusRequester,
    fallbackIndex = fallbackIndex,
    lazyIndexOffset = lazyIndexOffset,
    onRestoreFinished = onRestoreFinished,
    hasLayout = { listState.layoutInfo.visibleItemsInfo.isNotEmpty() },
    isItemVisible = { key, index ->
        listState.layoutInfo.visibleItemsInfo.any { it.key == key || it.index == index }
    },
    scrollToItem = { index -> listState.scrollToItem(index) },
)
