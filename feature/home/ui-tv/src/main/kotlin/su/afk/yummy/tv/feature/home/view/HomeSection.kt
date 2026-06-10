package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

@Composable
internal fun HomeSection(
    title: String,
    items: List<HomeFeedItem>,
    onItemSelected: (sectionId: String, item: HomeFeedItem) -> Unit,
    onItemFocused: (sectionId: String, displayId: Int, animeId: Int?) -> Unit,
    focusedItemId: Int?,
    focusedSectionId: String?,
    focusedPreview: AnimePreview?,
    rowFocusRequester: FocusRequester? = null,
    rowIsFocused: Boolean = false,
    rowKey: String = "",
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    bottomPadding: Dp = 20.dp,
    focusedCardScale: Float = 1.04f,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val rowHasFocusState = remember { mutableStateOf(false) }
    val isRestoringFocusState = remember { mutableStateOf(false) }
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    var focusMoveJob by remember { mutableStateOf<Job?>(null) }
    var lastFocusedIndex by rememberSaveable(rowKey.ifBlank { title }) { mutableIntStateOf(0) }
    val rowFocusRequesterToUse = rowFocusRequester ?: remember { FocusRequester() }
    val focusRequesters = remember(items.size, rowFocusRequesterToUse) {
        List(items.size) { FocusRequester() }
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

    fun moveFocusToIndex(targetIndex: Int) {
        if (items.isEmpty()) return
        val clamped = targetIndex.coerceIn(0, items.lastIndex)
        lastFocusedIndex = clamped
        focusMoveJob?.cancel()
        focusMoveJob = scope.launch {
            requestItemFocus(clamped)
        }
    }

    fun cancelPendingFocusMove() {
        focusMoveJob?.cancel()
        focusMoveJob = null
        isRestoringFocusState.value = false
    }

    LaunchedEffect(focusedSectionId, focusedItemId, items) {
        if (focusedSectionId != null && focusedSectionId != rowKey) return@LaunchedEffect
        val focusedIndex = items.indexOfFirst { it.id == focusedItemId }
        if (focusedSectionId != null && focusedIndex < 0) return@LaunchedEffect
        if (focusedIndex >= 0) {
            lastFocusedIndex = focusedIndex
            if (rowHasFocusState.value) {
                requestItemFocus(focusedIndex)
            }
        }
    }

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = TvScreenPadding.Horizontal),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
            contentPadding = PaddingValues(
                start = TvScreenPadding.Horizontal,
                end = TvScreenPadding.Horizontal,
                top = 8.dp,
                bottom = bottomPadding,
            ),
            modifier = Modifier
                .focusProperties {
                    upFocusRequester?.let { up = it }
                    mainMenuFocusRequester?.let { left = it }
                    downFocusRequester?.let { down = it }
                }
                .focusRequester(rowFocusRequesterToUse)
                .focusable()
                .onFocusChanged { state ->
                    val hadFocus = rowHasFocusState.value
                    rowHasFocusState.value = state.hasFocus
                    if (!state.hasFocus) cancelPendingFocusMove()
                    if (state.hasFocus && !hadFocus && items.isNotEmpty()) {
                        isRestoringFocusState.value = true
                        focusMoveJob?.cancel()
                        focusMoveJob = scope.launch {
                            val target = lastFocusedIndex.coerceIn(0, items.lastIndex)
                            requestItemFocus(target)
                            isRestoringFocusState.value = false
                            lastFocusedIndex = target
                            val focusedItem = items[target]
                            onItemFocused(rowKey, focusedItem.id, focusedItem.animeId)
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
                            items.getOrNull(lastFocusedIndex)?.let { item ->
                                onItemSelected(rowKey, item)
                            }
                            true
                        }

                        else -> false
                    }
                }
                .focusGroup(),
        ) {
            itemsIndexed(items = items, key = { _, item -> item.id }) { index, item ->
                val stableClick = remember(rowKey, item.id) { { onItemSelected(rowKey, item) } }
                val wrappedOnFocused = remember(index, item.id) {
                    { displayId: Int, animeId: Int? ->
                        if (!isRestoringFocusState.value) {
                            if (rowHasFocusState.value) {
                                lastFocusedIndex = index
                            }
                            onItemFocused(rowKey, displayId, animeId)
                        }
                    }
                }
                HomeFeedCard(
                    modifier = Modifier.focusRequester(focusRequesters[index]),
                    item = item,
                    preview = if (focusedSectionId == rowKey && item.id == focusedItemId) focusedPreview else null,
                    onClick = stableClick,
                    onFocused = wrappedOnFocused,
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = downFocusRequester,
                    focusedScale = focusedCardScale,
                    forceFocused = ((rowIsFocused || rowHasFocusState.value) && index == lastFocusedIndex) ||
                            (focusedSectionId == rowKey && item.id == focusedItemId),
                )
            }
        }
    }
}

private val HomeFeedItem.animeId: Int?
    get() = (action as? HomeFeedItemAction.OpenSeries)?.seriesId
