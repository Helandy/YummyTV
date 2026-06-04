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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

@Composable
internal fun HomeSection(
    title: String,
    items: List<HomeFeedItem>,
    onItemSelected: (HomeFeedItem) -> Unit,
    onItemFocused: (displayId: Int, animeId: Int?) -> Unit,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    rowFocusRequester: FocusRequester? = null,
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
    var lastFocusedIndex by rememberSaveable(title) { mutableIntStateOf(0) }
    val focusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }

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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = TvScreenPadding.Horizontal,
                end = TvScreenPadding.Horizontal,
                top = 8.dp,
                bottom = bottomPadding,
            ),
            modifier = Modifier
                .then(if (rowFocusRequester != null) Modifier.focusRequester(rowFocusRequester) else Modifier)
                .focusProperties {
                    upFocusRequester?.let { up = it }
                    mainMenuFocusRequester?.let { left = it }
                    downFocusRequester?.let { down = it }
                }
                .onFocusChanged { state ->
                    val hadFocus = rowHasFocusState.value
                    rowHasFocusState.value = state.hasFocus
                    if (!state.hasFocus) {
                        isRestoringFocusState.value = false
                    }
                    if (state.hasFocus && !hadFocus && items.isNotEmpty()) {
                        isRestoringFocusState.value = true
                        scope.launch {
                            val target = lastFocusedIndex.coerceIn(0, items.lastIndex)
                            listState.scrollToItem(target)
                            val focusRestored = runCatching { focusRequesters[target].requestFocus() }.isSuccess
                            isRestoringFocusState.value = false
                            if (focusRestored) {
                                lastFocusedIndex = target
                                val focusedItem = items[target]
                                onItemFocused(focusedItem.id, focusedItem.animeId)
                            }
                        }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (lastFocusedIndex <= 0) {
                                mainMenuFocusRequester?.requestFocus()
                                mainMenuFocusRequester != null
                            } else {
                                false
                            }
                        }
                        Key.DirectionRight -> lastFocusedIndex >= items.lastIndex
                        Key.DirectionUp -> {
                            onMoveUp?.invoke()
                            onMoveUp != null
                        }
                        Key.DirectionDown -> {
                            onMoveDown?.invoke()
                            onMoveDown != null
                        }
                        else -> false
                    }
                }
                .focusGroup(),
        ) {
            itemsIndexed(items = items, key = { _, item -> item.id }) { index, item ->
                val stableClick = remember(item.id) { { onItemSelected(item) } }
                val wrappedOnFocused = remember(index, item.id) {
                    { displayId: Int, animeId: Int? ->
                        if (!isRestoringFocusState.value) {
                            if (rowHasFocusState.value) {
                                lastFocusedIndex = index
                            }
                            onItemFocused(displayId, animeId)
                        }
                    }
                }
                HomeFeedCard(
                    modifier = Modifier.focusRequester(focusRequesters[index]),
                    item = item,
                    preview = if (item.id == focusedItemId) focusedPreview else null,
                    onClick = stableClick,
                    onFocused = wrappedOnFocused,
                    upFocusRequester = upFocusRequester,
                    downFocusRequester = downFocusRequester,
                    focusedScale = focusedCardScale,
                )
            }
        }
    }
}

private val HomeFeedItem.animeId: Int?
    get() = (action as? HomeFeedItemAction.OpenSeries)?.seriesId
