package su.afk.yummy.tv.feature.top100.utils

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val FocusRestoreTimeoutMillis = 500L
private const val FocusRestoreInitialFrameWait = 2

internal fun launchRestoreTop100ItemFocus(
    previousJob: Job?,
    scope: CoroutineScope,
    itemIndex: Int,
    gridState: LazyGridState,
    itemFocusRequesters: List<FocusRequester>,
    fallbackFocusRequester: FocusRequester,
    onRestoreFinished: () -> Unit = {},
): Job {
    previousJob?.cancel()
    return scope.launch {
        try {
            val focusRestored = restoreTop100ItemFocus(
                itemIndex = itemIndex,
                gridState = gridState,
                itemFocusRequesters = itemFocusRequesters,
            )
            if (!focusRestored) {
                requestFocusUntilTimeout(fallbackFocusRequester)
            }
            onRestoreFinished()
        } catch (e: CancellationException) {
            throw e
        }
    }
}

private suspend fun restoreTop100ItemFocus(
    itemIndex: Int,
    gridState: LazyGridState,
    itemFocusRequesters: List<FocusRequester>,
): Boolean {
    val itemFocusRequester = itemFocusRequesters.getOrNull(itemIndex) ?: return false

    return withTimeoutOrNull(FocusRestoreTimeoutMillis) {
        repeat(FocusRestoreInitialFrameWait) {
            withFrameNanos { }
        }
        gridState.scrollToItem(itemIndex)
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }
        }.first { it }

        requestFocusUntilTimeout(itemFocusRequester)
    } ?: false
}

private suspend fun requestFocusUntilTimeout(
    requester: FocusRequester,
): Boolean =
    withTimeoutOrNull<Boolean>(FocusRestoreTimeoutMillis) {
        var focused = false
        while (!focused) {
            withFrameNanos { }
            focused = runCatching { requester.requestFocus() }.getOrDefault(false)
        }
        focused
    } ?: false
