@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.utils.isEmpty

@Composable
internal fun StatsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    selectedTabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val stats = state.stats
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> {
                        onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.NOTIFICATIONS))
                        true
                    }

                    Key.DirectionDown -> {
                        val last = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        if (listState.firstVisibleItemIndex >= last) return@onPreviewKeyEvent false
                        val next = (listState.firstVisibleItemIndex + 1).coerceAtMost(last)
                        scope.launch { listState.scrollToItem(next) }
                        true
                    }

                    Key.DirectionUp -> {
                        if (listState.firstVisibleItemIndex == 0) return@onPreviewKeyEvent false
                        val previous = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                        scope.launch { listState.scrollToItem(previous) }
                        true
                    }

                    else -> false
                }
            },
    ) {
        item {
            AccountHeader(state = state, onEvent = onEvent)
        }
        item {
            AccountTabs(
                selected = state.selectedTab,
                onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                selectedTabFocusRequester = selectedTabFocusRequester,
                contentFocusRequester = focusRequester,
            )
        }
        item {
            ErrorText(state.error ?: state.hubError)
        }
        if (state.isStatsLoading && stats == null) {
            item { EmptyText(stringResource(R.string.account_loading)) }
            return@LazyColumn
        }
        if (stats == null || stats.isEmpty()) {
            item { EmptyText(stringResource(R.string.account_stats_empty)) }
            return@LazyColumn
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                stats.lists.forEach { ListStatCard(it) }
            }
        }
        item { GenreStats(stats.genres) }
        item { RatingStats(stats.ratings) }
        item { TypeStats(stats.types) }
    }
}
