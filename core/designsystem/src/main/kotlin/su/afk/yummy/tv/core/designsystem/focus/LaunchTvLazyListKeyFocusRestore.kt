package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

private val TvLazyGridFocusRestoreTimeout = 500.milliseconds
private const val TvLazyGridFocusRestoreInitialFrameWait = 2

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
): Job {
    previousJob?.cancel()
    return scope.launch {
        val targetIndex = restoreState.targetIndex(keys)
            ?: fallbackIndex.takeIf { it >= 0 && it < keys.size }
        val focusRestored = targetIndex?.let { index ->
            restoreTvLazyListKeyFocus(
                itemKey = keys[index],
                itemIndex = index + lazyIndexOffset,
                listState = listState,
                itemFocusRequesters = itemFocusRequesters,
            )
        } ?: false
        if (!focusRestored) {
            requestFocusUntilTimeout(fallbackFocusRequester)
        }
        onRestoreFinished()
    }
}

private suspend fun <Key : Any> restoreTvLazyListKeyFocus(
    itemKey: Key,
    itemIndex: Int,
    listState: LazyListState,
    itemFocusRequesters: Map<Key, FocusRequester>,
): Boolean {
    val itemFocusRequester = itemFocusRequesters[itemKey] ?: return false

    return withTimeoutOrNull(TvLazyGridFocusRestoreTimeout) {
        repeat(TvLazyGridFocusRestoreInitialFrameWait) {
            withFrameNanos { }
        }
        if (
            listState.layoutInfo.visibleItemsInfo.any { itemInfo ->
                itemInfo.key == itemKey || itemInfo.index == itemIndex
            } &&
            requestFocusForFrames(itemFocusRequester)
        ) {
            return@withTimeoutOrNull true
        }
        listState.scrollToItem(itemIndex)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { itemInfo ->
                itemInfo.key == itemKey || itemInfo.index == itemIndex
            }
        }.first { it }

        requestFocusUntilTimeout(itemFocusRequester)
    } ?: false
}

private suspend fun requestFocusForFrames(
    requester: FocusRequester,
): Boolean {
    repeat(TvLazyGridFocusRestoreInitialFrameWait) {
        withFrameNanos { }
        if (runCatching { requester.requestFocus() }.getOrDefault(false)) {
            return true
        }
    }
    return false
}
