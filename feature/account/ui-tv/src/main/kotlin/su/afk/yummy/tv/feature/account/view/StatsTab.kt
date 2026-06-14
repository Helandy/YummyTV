@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.utils.isEmpty

@Composable
internal fun StatsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    selectedTabFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val stats = state.stats
    val profileSummary = state.profileSummary
    val listState = rememberLazyListState()
    val listFocusRequester = remember { FocusRequester() }
    val profileOverviewFocusRequester = remember { FocusRequester() }
    val contentFocusRequester =
        if (profileSummary != null) profileOverviewFocusRequester else listFocusRequester
    val scope = rememberCoroutineScope()
    var isProfileOverviewFocused by remember { mutableStateOf(false) }

    fun requestProfileOverviewFocus() {
        scope.launch {
            listState.scrollToItem(PROFILE_OVERVIEW_ITEM_INDEX)
            repeat(6) {
                runCatching { profileOverviewFocusRequester.requestFocus() }
                withFrameNanos { }
            }
        }
    }

    fun scrollByProfilePage(direction: Int) {
        scope.launch {
            listState.animateScrollBy(listState.profilePageScrollPx() * direction)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .focusRequester(listFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> {
                        onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.NOTIFICATIONS))
                        true
                    }

                    Key.DirectionDown -> {
                        if (profileSummary != null && !isProfileOverviewFocused) {
                            requestProfileOverviewFocus()
                            return@onPreviewKeyEvent true
                        }
                        if (!listState.canScrollForward) return@onPreviewKeyEvent false
                        scrollByProfilePage(direction = 1)
                        true
                    }

                    Key.DirectionUp -> {
                        if (!listState.canScrollBackward) return@onPreviewKeyEvent false
                        scrollByProfilePage(direction = -1)
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
                contentFocusRequester = contentFocusRequester,
            )
        }
        item {
            ErrorText((state.error ?: state.hubError).accountErrorMessage())
        }
        if (state.isStatsLoading && stats == null && profileSummary == null) {
            item { EmptyText(stringResource(R.string.account_loading)) }
            return@LazyColumn
        }
        if (stats == null && profileSummary == null) {
            item { EmptyText(stringResource(R.string.account_stats_empty)) }
            return@LazyColumn
        }
        if (profileSummary != null) {
            item {
                AccountProfileOverviewPanel(
                    summary = profileSummary,
                    stats = stats,
                    statsGridFocusRequester = profileOverviewFocusRequester,
                    onStatsGridFocusChanged = { isProfileOverviewFocused = it },
                )
            }
        } else if (stats != null && !stats.isEmpty()) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    stats.lists.forEach { ListStatCard(it) }
                }
            }
        }
        if (stats == null || stats.isEmpty()) {
            return@LazyColumn
        }
        if (profileSummary == null && stats.genres.isNotEmpty()) item { GenreStats(stats.genres) }
        if (profileSummary == null && stats.ratings.isNotEmpty()) item { RatingStats(stats.ratings) }
    }
}

private fun LazyListState.profilePageScrollPx(): Float =
    (layoutInfo.viewportSize.height * PROFILE_PAGE_SCROLL_FRACTION)
        .coerceAtLeast(PROFILE_MIN_PAGE_SCROLL_PX)

private const val PROFILE_OVERVIEW_ITEM_INDEX = 3
private const val PROFILE_PAGE_SCROLL_FRACTION = 0.72f
private const val PROFILE_MIN_PAGE_SCROLL_PX = 240f
