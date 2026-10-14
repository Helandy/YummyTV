package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

private val TvLazyFocusRestoreTimeout = 500.milliseconds
private const val TvLazyFocusRestoreScaffoldFrames = 2

/**
 * Lazy-layout-agnostic core of [launchTvLazyListKeyFocusRestore] / [launchTvLazyGridKeyFocusRestore]:
 * the list/grid specifics come in as [hasLayout], [isItemVisible] and [scrollToItem].
 */
internal fun <Key : Any> launchTvLazyKeyFocusRestore(
    previousJob: Job?,
    scope: CoroutineScope,
    restoreState: TvLazyFocusRestoreState<Key>,
    keys: List<Key>,
    itemFocusRequesters: Map<Key, FocusRequester>,
    fallbackFocusRequester: FocusRequester,
    fallbackIndex: Int,
    lazyIndexOffset: Int,
    onRestoreFinished: () -> Unit,
    hasLayout: () -> Boolean,
    isItemVisible: (key: Key, index: Int) -> Boolean,
    scrollToItem: suspend (index: Int) -> Unit,
): Job {
    previousJob?.cancel()
    return scope.launch {
        val targetIndex = restoreState.targetIndex(keys)
            ?: fallbackIndex.takeIf { it >= 0 && it < keys.size }
        val focusRestored = targetIndex?.let { index ->
            val itemKey = keys[index]
            val itemIndex = index + lazyIndexOffset
            val itemFocusRequester = itemFocusRequesters[itemKey] ?: return@let false
            withTimeoutOrNull(TvLazyFocusRestoreTimeout) {
                // Not a readiness wait: on returning to the screen the main scaffold hands focus to
                // the screen's preferred requester over the next frames. Restoring earlier lets
                // that handoff override the restored item (focus lands on a neighbour).
                repeat(TvLazyFocusRestoreScaffoldFrames) { withFrameNanos { } }
                // Right after (re)entering the screen the layout may not be measured yet: an empty
                // visibleItemsInfo would look like "item off-screen" and trigger a needless scroll.
                snapshotFlow { hasLayout() }.first { it }
                if (!isItemVisible(itemKey, itemIndex)) {
                    scrollToItem(itemIndex)
                    snapshotFlow { isItemVisible(itemKey, itemIndex) }.first { it }
                }
                requestFocusUntilTimeout(itemFocusRequester)
            } ?: false
        } ?: false
        if (!focusRestored) {
            requestFocusUntilTimeout(fallbackFocusRequester)
        }
        onRestoreFinished()
    }
}
