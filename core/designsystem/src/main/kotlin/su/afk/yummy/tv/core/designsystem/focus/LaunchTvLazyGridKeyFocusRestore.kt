package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.lazy.grid.LazyGridState
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

fun <Key : Any> launchTvLazyGridKeyFocusRestore(
    previousJob: Job?,
    scope: CoroutineScope,
    restoreState: TvLazyFocusRestoreState<Key>,
    keys: List<Key>,
    gridState: LazyGridState,
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
            restoreTvLazyGridKeyFocus(
                itemKey = keys[index],
                itemIndex = index + lazyIndexOffset,
                gridState = gridState,
                itemFocusRequesters = itemFocusRequesters,
            )
        } ?: false
        if (!focusRestored) {
            requestFocusUntilTimeout(fallbackFocusRequester)
        }
        onRestoreFinished()
    }
}

private suspend fun <Key : Any> restoreTvLazyGridKeyFocus(
    itemKey: Key,
    itemIndex: Int,
    gridState: LazyGridState,
    itemFocusRequesters: Map<Key, FocusRequester>,
): Boolean {
    val itemFocusRequester = itemFocusRequesters[itemKey] ?: return false

    return withTimeoutOrNull(TvLazyGridFocusRestoreTimeout) {
        repeat(TvLazyGridFocusRestoreInitialFrameWait) {
            withFrameNanos { }
        }
        if (
            gridState.layoutInfo.visibleItemsInfo.any { itemInfo ->
                itemInfo.key == itemKey || itemInfo.index == itemIndex
            } &&
            requestFocusForFrames(itemFocusRequester)
        ) {
            return@withTimeoutOrNull true
        }
        gridState.scrollToItem(itemIndex)
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.any { itemInfo ->
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
