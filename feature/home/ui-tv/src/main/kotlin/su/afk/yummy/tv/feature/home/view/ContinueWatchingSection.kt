package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.feature.home.R

@Composable
internal fun ContinueWatchingSection(
    items: List<WatchProgressEntry>,
    onItemSelected: (WatchProgressEntry) -> Unit,
    rowFocusRequester: FocusRequester? = null,
    restoreFirstItemToken: Int = 0,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val rowHasFocus = remember { mutableStateOf(false) }
    val isRestoring = remember { mutableStateOf(false) }
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    var focusMoveJob by remember { mutableStateOf<Job?>(null) }
    var lastFocusedIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var handledRestoreFirstItemToken by rememberSaveable { mutableIntStateOf(0) }
    val focusRequesters = remember(items.size, rowFocusRequester) {
        List(items.size) { index ->
            if (index == 0) rowFocusRequester ?: FocusRequester() else FocusRequester()
        }
    }

    fun moveFocusToIndex(targetIndex: Int) {
        if (items.isEmpty()) return
        val clamped = targetIndex.coerceIn(0, items.lastIndex)
        lastFocusedIndex = clamped
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch {
            listState.animateScrollToItem(clamped)
            runCatching { focusRequesters[clamped].requestFocus() }
        }
    }

    fun cancelPendingFocusMove() {
        focusMoveJob?.cancel()
        focusMoveJob = null
        isRestoring.value = false
    }

    fun WatchProgressEntry.focusKey(): String = "$animeId:$episode"

    fun hasPendingFirstItemRestore(): Boolean = restoreFirstItemToken > handledRestoreFirstItemToken

    fun restoreIndex(): Int {
        if (hasPendingFirstItemRestore()) return 0
        val keyedIndex = lastFocusedKey?.let { key ->
            items.indexOfFirst { it.focusKey() == key }
        } ?: -1
        return keyedIndex.takeIf { it >= 0 } ?: lastFocusedIndex.coerceIn(0, items.lastIndex)
    }

    fun rememberFocusedItem(index: Int, entry: WatchProgressEntry) {
        lastFocusedIndex = index
        lastFocusedKey = entry.focusKey()
    }

    Column {
        Text(
            text = stringResource(R.string.continue_watching),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = TvScreenPadding.Horizontal),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = TvScreenPadding.Horizontal, vertical = 8.dp),
            modifier = Modifier
                .focusProperties {
                    upFocusRequester?.let { up = it }
                    mainMenuFocusRequester?.let { left = it }
                    downFocusRequester?.let { down = it }
                }
                .onFocusChanged { state ->
                    val hadFocus = rowHasFocus.value
                    rowHasFocus.value = state.hasFocus
                    if (!state.hasFocus) cancelPendingFocusMove()
                    if (state.hasFocus && !hadFocus) {
                        isRestoring.value = true
                        focusMoveJob?.cancel()
                        focusMoveJob = scope.launch {
                            if (hasPendingFirstItemRestore()) {
                                handledRestoreFirstItemToken = restoreFirstItemToken
                                rememberFocusedItem(0, items.first())
                            }
                            val target = restoreIndex().coerceIn(0, items.lastIndex)
                            listState.scrollToItem(target)
                            runCatching { focusRequesters[target].requestFocus() }
                            isRestoring.value = false
                        }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (lastFocusedIndex <= 0) {
                                cancelPendingFocusMove()
                                mainMenuFocusRequester?.requestFocus()
                                mainMenuFocusRequester != null
                            } else {
                                moveFocusToIndex(lastFocusedIndex - 1)
                                true
                            }
                        }

                        Key.DirectionRight -> {
                            if (lastFocusedIndex < items.lastIndex) {
                                moveFocusToIndex(lastFocusedIndex + 1)
                                true
                            } else {
                                false
                            }
                        }

                        Key.DirectionUp -> {
                            cancelPendingFocusMove()
                            onMoveUp?.invoke()
                            onMoveUp != null
                        }

                        Key.DirectionDown -> {
                            cancelPendingFocusMove()
                            onMoveDown?.invoke()
                            onMoveDown != null
                        }

                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            items.getOrNull(lastFocusedIndex)?.let { entry ->
                                rememberFocusedItem(lastFocusedIndex, entry)
                                onItemSelected(entry)
                            }
                            true
                        }

                        else -> false
                    }
                }
                .focusGroup(),
        ) {
            itemsIndexed(items = items, key = { _, e -> e.episodeUrl }) { index, entry ->
                ContinueWatchingCard(
                    entry = entry,
                    onFocused = {
                        if (rowHasFocus.value && !isRestoring.value) rememberFocusedItem(index, entry)
                    },
                    onClick = {
                        rememberFocusedItem(index, entry)
                        onItemSelected(entry)
                    },
                    modifier = Modifier.focusRequester(focusRequesters[index]),
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = downFocusRequester,
                )
            }
        }
    }
}
