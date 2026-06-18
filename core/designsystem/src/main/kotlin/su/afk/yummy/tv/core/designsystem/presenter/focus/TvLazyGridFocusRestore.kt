package su.afk.yummy.tv.core.designsystem.presenter.focus

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TvLazyGridFocusRestoreTimeoutMillis = 500L
private const val TvLazyGridFocusRestoreInitialFrameWait = 2

@Stable
class TvLazyFocusRestoreState<Key : Any> internal constructor(
    private val savedKeyState: MutableState<Key?>,
    private val savedIndexState: MutableIntState,
) {
    val savedKey: Key?
        get() = savedKeyState.value

    val savedIndex: Int
        get() = savedIndexState.intValue

    fun onItemFocused(key: Key, index: Int) {
        savedKeyState.value = key
        savedIndexState.intValue = index
    }

    fun clear() {
        savedKeyState.value = null
        savedIndexState.intValue = 0
    }

    fun targetIndex(keys: List<Key>): Int? {
        if (keys.isEmpty()) return null
        val key = savedKey
        if (key != null) {
            val keyIndex = keys.indexOf(key)
            if (keyIndex >= 0) return keyIndex
        }
        return savedIndex.coerceIn(0, keys.lastIndex)
    }
}

@Composable
fun <Key : Any> rememberTvLazyFocusRestoreState(
    vararg inputs: Any?,
): TvLazyFocusRestoreState<Key> {
    val savedKeyState = rememberSaveable(*inputs) { mutableStateOf<Key?>(null) }
    val savedIndexState = rememberSaveable(*inputs) { mutableIntStateOf(0) }
    return remember(*inputs) { TvLazyFocusRestoreState(savedKeyState, savedIndexState) }
}

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
        try {
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
        } catch (e: CancellationException) {
            throw e
        }
    }
}

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
        try {
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
        } catch (e: CancellationException) {
            throw e
        }
    }
}

private suspend fun <Key : Any> restoreTvLazyGridKeyFocus(
    itemKey: Key,
    itemIndex: Int,
    gridState: LazyGridState,
    itemFocusRequesters: Map<Key, FocusRequester>,
): Boolean {
    val itemFocusRequester = itemFocusRequesters[itemKey] ?: return false

    return withTimeoutOrNull(TvLazyGridFocusRestoreTimeoutMillis) {
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

private suspend fun <Key : Any> restoreTvLazyListKeyFocus(
    itemKey: Key,
    itemIndex: Int,
    listState: LazyListState,
    itemFocusRequesters: Map<Key, FocusRequester>,
): Boolean {
    val itemFocusRequester = itemFocusRequesters[itemKey] ?: return false

    return withTimeoutOrNull(TvLazyGridFocusRestoreTimeoutMillis) {
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

private suspend fun requestFocusUntilTimeout(
    requester: FocusRequester,
): Boolean =
    withTimeoutOrNull(TvLazyGridFocusRestoreTimeoutMillis) {
        var focused = false
        while (!focused) {
            withFrameNanos { }
            focused = runCatching { requester.requestFocus() }.getOrDefault(false)
        }
        focused
    } ?: false
