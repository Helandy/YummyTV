package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.home.R

@Composable
internal fun ContinueWatchingSection(
    items: List<HomeContinueWatchingItem>,
    onItemSelected: (HomeContinueWatchingItem) -> Unit,
    rowFocusRequester: FocusRequester? = null,
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
    var currentFocusedIndex by remember { mutableIntStateOf(-1) }
    val focusRequesters = remember(items.size, rowFocusRequester) {
        List(items.size) { index ->
            if (index == 0) rowFocusRequester ?: FocusRequester() else FocusRequester()
        }
    }

    suspend fun requestItemFocus(index: Int) {
        val target = index.coerceIn(0, items.lastIndex)
        val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == target }
        if (!isVisible) {
            listState.scrollToItem(target)
            withFrameNanos { }
        }
        runCatching { focusRequesters[target].requestFocus() }
        withFrameNanos { }
        runCatching { focusRequesters[target].requestFocus() }
    }

    fun cancelPendingFocusMove() {
        focusMoveJob?.cancel()
        focusMoveJob = null
        isRestoring.value = false
    }

    fun HomeContinueWatchingItem.focusKey(): String = "$animeId:$videoId:$episode:$episodeUrl"

    fun restoreIndex(): Int {
        val keyedIndex = lastFocusedKey?.let { key ->
            items.indexOfFirst { it.focusKey() == key }
        } ?: -1
        return keyedIndex.takeIf { it >= 0 } ?: lastFocusedIndex.coerceIn(0, items.lastIndex)
    }

    fun rememberFocusedItem(index: Int, entry: HomeContinueWatchingItem) {
        currentFocusedIndex = index
        lastFocusedIndex = index
        lastFocusedKey = entry.focusKey()
    }

    Column {
        HomeSectionHeader(
            title = stringResource(R.string.continue_watching),
            active = rowHasFocus.value,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
            contentPadding = PaddingValues(horizontal = TvScreenPadding.Horizontal, vertical = 8.dp),
            modifier = Modifier
                .focusProperties {
                    upFocusRequester?.let { up = it }
                    downFocusRequester?.let { down = it }
                }
                .onFocusChanged { state ->
                    val hadFocus = rowHasFocus.value
                    rowHasFocus.value = state.hasFocus
                    if (!state.hasFocus) {
                        currentFocusedIndex = -1
                        cancelPendingFocusMove()
                    }
                    if (state.hasFocus && !hadFocus) {
                        isRestoring.value = true
                        focusMoveJob?.cancel()
                        focusMoveJob = scope.launch {
                            val target = restoreIndex().coerceIn(0, items.lastIndex)
                            requestItemFocus(target)
                            items.getOrNull(target)?.let { rememberFocusedItem(target, it) }
                            isRestoring.value = false
                        }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            val focusedIndex = currentFocusedIndex
                                .takeIf { it in items.indices }
                                ?: lastFocusedIndex
                            if (focusedIndex <= 0) {
                                cancelPendingFocusMove()
                                mainMenuFocusRequester?.requestFocus()
                                mainMenuFocusRequester != null
                            } else {
                                false
                            }
                        }

                        Key.DirectionRight -> false

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

                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> false

                        else -> false
                    }
                }
                .tvFocusRestorer(
                    fallback = focusRequesters.getOrNull(restoreIndex()) ?: FocusRequester.Default,
                ),
        ) {
            itemsIndexed(items = items, key = { _, e -> e.focusKey() }) { index, entry ->
                ContinueWatchingCard(
                    entry = entry,
                    onFocused = {
                        currentFocusedIndex = index
                        if (rowHasFocus.value && !isRestoring.value) rememberFocusedItem(index, entry)
                    },
                    onClick = {
                        rememberFocusedItem(index, entry)
                        onItemSelected(entry)
                    },
                    modifier = Modifier.focusRequester(focusRequesters[index]),
                    leftFocusRequester = mainMenuFocusRequester.takeIf { index == 0 },
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = downFocusRequester,
                )
            }
        }
    }
}
