@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.domain.account.model.ratingsByValue
import su.afk.yummy.tv.domain.account.model.topGenres
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.utils.isEmpty

@Composable
internal fun StatsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    selectedTabFocusRequester: FocusRequester? = null,
    onStatsContentFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val stats = state.stats
    val profileSummary = state.profileSummary
    val listState = rememberLazyListState()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val listFocusRequester = remember { FocusRequester() }
    val fallbackSelectedTabFocusRequester = remember { FocusRequester() }
    val statsTabFocusRequester = selectedTabFocusRequester ?: fallbackSelectedTabFocusRequester
    val profileOverviewFocusRequester = remember { FocusRequester() }
    val statsGridBottomStartFocusRequester = remember { FocusRequester() }
    val daysOnlineFocusRequester = remember { FocusRequester() }
    val listCountersFocusRequester = remember { FocusRequester() }
    val contentFocusRequester =
        if (profileSummary != null) profileOverviewFocusRequester else listFocusRequester
    val scope = rememberCoroutineScope()
    var isProfileOverviewFocused by remember { mutableStateOf(false) }
    var isHeaderFocused by remember { mutableStateOf(false) }

    fun requestProfileOverviewFocus() {
        scope.launch {
            listState.scrollToItem(PROFILE_OVERVIEW_ITEM_INDEX)
            requestFocusUntilTimeout(profileOverviewFocusRequester)
        }
    }

    fun scrollByProfilePage(direction: Int): Boolean {
        if (direction > 0 && !listState.canScrollForward) return false
        if (direction < 0 && !listState.canScrollBackward) return false
        scope.launch {
            listState.animateScrollBy(listState.profilePageScrollPx() * direction)
        }
        return true
    }

    fun requestDaysOnlineFocus(): Boolean {
        scope.launch { requestFocusUntilTimeout(daysOnlineFocusRequester) }
        return true
    }

    fun requestMainMenuFocus(): Boolean {
        val requester = mainMenuFocusRequester ?: return false
        scope.launch { requestFocusUntilTimeout(requester) }
        return true
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
                    Key.DirectionLeft -> {
                        if (isProfileOverviewFocused) return@onPreviewKeyEvent false
                        requestMainMenuFocus()
                    }

                    Key.DirectionRight -> {
                        if (isProfileOverviewFocused) return@onPreviewKeyEvent false
                        onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.NOTIFICATIONS))
                        true
                    }

                    Key.DirectionDown -> {
                        // С шапки («Выйти») вниз — на выбранный таб, а не в карточки статистики.
                        if (isHeaderFocused) {
                            scope.launch { requestFocusUntilTimeout(statsTabFocusRequester) }
                            return@onPreviewKeyEvent true
                        }
                        if (profileSummary != null && !isProfileOverviewFocused) {
                            requestProfileOverviewFocus()
                            return@onPreviewKeyEvent true
                        }
                        if (isProfileOverviewFocused) return@onPreviewKeyEvent false
                        scrollByProfilePage(direction = 1)
                    }

                    Key.DirectionUp -> {
                        if (isHeaderFocused) return@onPreviewKeyEvent false
                        if (isProfileOverviewFocused) return@onPreviewKeyEvent false
                        scrollByProfilePage(direction = -1)
                    }

                    else -> false
                }
            },
    ) {
        item {
            Box(
                modifier = Modifier.onFocusChanged { isHeaderFocused = it.hasFocus },
            ) {
                AccountHeader(
                    state = state,
                    onEvent = onEvent,
                    downFocusRequester = statsTabFocusRequester,
                )
            }
        }
        item {
            AccountTabs(
                selected = state.selectedTab,
                onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                unreadCount = state.unreadNotificationCount,
                selectedTabFocusRequester = statsTabFocusRequester,
                contentFocusRequester = contentFocusRequester,
                autoFocusSelected = !isProfileOverviewFocused,
            )
        }
        item {
            AccountHubError(
                error = (
                    state.error
                        ?: state.hubError.takeIf { it != AccountUiError.OPEN_NOTIFICATION_FAILED }
                    ).accountErrorMessage(),
                onRetry = { onEvent(AccountState.Event.RefreshHubSelected) },
            )
        }
        if (state.isStatsLoading && stats == null && profileSummary == null) {
            item { TvLoadingScreen(modifier = Modifier.height(360.dp)) }
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
                    statsGridBottomStartFocusRequester = statsGridBottomStartFocusRequester,
                    statsGridTopExitFocusRequester = statsTabFocusRequester,
                    daysOnlineFocusRequester = daysOnlineFocusRequester,
                    listCountersFocusRequester = listCountersFocusRequester,
                    onContentFocusChanged = {
                        isProfileOverviewFocused = it
                        onStatsContentFocusChanged(it)
                    },
                    onStatsGridExitRight = {
                        onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.NOTIFICATIONS))
                        true
                    },
                    onStatsGridExitDown = ::requestDaysOnlineFocus,
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
        if (profileSummary == null && stats.genres.isNotEmpty()) item { GenreStats(stats.topGenres) }
        if (profileSummary == null && stats.ratings.isNotEmpty()) item { RatingStats(stats.ratingsByValue) }
    }
}

private fun LazyListState.profilePageScrollPx(): Float =
    (layoutInfo.viewportSize.height * PROFILE_PAGE_SCROLL_FRACTION)
        .coerceAtLeast(PROFILE_MIN_PAGE_SCROLL_PX)

private const val PROFILE_OVERVIEW_ITEM_INDEX = 3
private const val PROFILE_PAGE_SCROLL_FRACTION = 0.72f
private const val PROFILE_MIN_PAGE_SCROLL_PX = 240f
