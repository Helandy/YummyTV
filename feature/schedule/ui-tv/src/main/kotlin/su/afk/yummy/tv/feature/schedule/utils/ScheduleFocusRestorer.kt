package su.afk.yummy.tv.feature.schedule.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

private const val FocusRestoreTimeoutMillis = 500L
private const val FocusRestoreFrameAttempts = 3

internal fun launchRestoreTimelineFocus(
    previousJob: Job?,
    scope: CoroutineScope,
    focusedReleaseKey: String?,
    selectedGroup: ScheduleDayUi,
    listState: LazyListState,
    releaseFocusRequesters: Map<String, FocusRequester>,
    fallbackFocusRequester: FocusRequester,
): Job {
    previousJob?.cancel()
    return scope.launch {
        val focusRestored = restoreFocusedReleaseFocus(
            focusedReleaseKey = focusedReleaseKey,
            selectedGroup = selectedGroup,
            listState = listState,
            releaseFocusRequesters = releaseFocusRequesters,
        )
        if (!focusRestored) {
            runCatching { fallbackFocusRequester.requestFocus() }
        }
    }
}

private suspend fun restoreFocusedReleaseFocus(
    focusedReleaseKey: String?,
    selectedGroup: ScheduleDayUi,
    listState: LazyListState,
    releaseFocusRequesters: Map<String, FocusRequester>,
): Boolean {
    val releaseKey = focusedReleaseKey ?: return false
    val releaseIndex = selectedGroup.items.indexOfFirst { it.focusKey == releaseKey }
    if (releaseIndex < 0) return false

    return withTimeoutOrNull(FocusRestoreTimeoutMillis) {
        listState.scrollToItem(releaseIndex)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.any { it.index == releaseIndex }
        }.first { it }

        val releaseFocusRequester = snapshotFlow { releaseFocusRequesters[releaseKey] }
            .filterNotNull()
            .first()

        repeat(FocusRestoreFrameAttempts) {
            withFrameNanos { }
            val focusRequested = runCatching { releaseFocusRequester.requestFocus() }
                .getOrDefault(false)
            if (focusRequested) {
                return@withTimeoutOrNull true
            }
        }
        false
    } ?: false
}
