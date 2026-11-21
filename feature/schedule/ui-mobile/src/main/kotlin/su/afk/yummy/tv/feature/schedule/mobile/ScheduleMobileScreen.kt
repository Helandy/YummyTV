package su.afk.yummy.tv.feature.schedule.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.components.OfflineBanner
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.schedule.ScheduleState
import su.afk.yummy.tv.feature.schedule.mobile.view.ScheduleMobileDateChips
import su.afk.yummy.tv.feature.schedule.mobile.view.ScheduleMobileLibraryFilterChip
import su.afk.yummy.tv.feature.schedule.mobile.view.ScheduleMobileReleaseCard
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi
import su.afk.yummy.tv.feature.schedule.presentation.R
import java.time.ZoneId
import java.time.ZonedDateTime

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScheduleMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        ScheduleMobileScreen(ScheduleState.State(isLoading = false), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun ScheduleMobileScreenLoadingPreview() = ScreenPreviewTheme {
    ScheduleMobileScreen(ScheduleState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun ScheduleMobileScreenErrorPreview() = ScreenPreviewTheme {
    ScheduleMobileScreen(
        ScheduleState.State(
            isLoading = false,
            error = UiText.Failure(IOException()),
        ),
        emptyFlow(),
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScheduleMobileScreen(
    state: ScheduleState.State,
    effect: Flow<ScheduleState.Effect>,
    onEvent: (ScheduleState.Event) -> Unit,
) {
    val schedule = state.tvSchedule
    val dayGroups = schedule.dayGroups
    val selectedPage = dayGroups
        .indexOfFirst { it.date.toEpochDay() == schedule.selectedEpochDay }
        .coerceAtLeast(0)
    val currentDayGroups by rememberUpdatedState(dayGroups)
    val currentSelectedEpochDay by rememberUpdatedState(schedule.selectedEpochDay)
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = selectedPage,
        pageCount = dayGroups.size.coerceAtLeast(1),
        onPageSelected = { page ->
            val epochDay = currentDayGroups.getOrNull(page)?.date?.toEpochDay()
            if (epochDay != null && epochDay != currentSelectedEpochDay) {
                onEvent(ScheduleState.Event.DateSelected(epochDay))
            }
        },
    )

    LaunchedEffect(state.onlyInLibrary) {
        if (dayGroups.isNotEmpty() && tabsState.pagerState.currentPage != selectedPage) {
            tabsState.pagerState.scrollToPage(selectedPage)
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.schedule_mobile_title),
                onBack = { onEvent(ScheduleState.Event.BackSelected) },
                actions = {
                    val showFilterChip = !state.isLoading &&
                        state.error == null &&
                        (dayGroups.isNotEmpty() || state.onlyInLibrary)
                    if (showFilterChip) {
                        ScheduleMobileLibraryFilterChip(
                            selected = state.onlyInLibrary,
                            onToggle = { onEvent(ScheduleState.Event.InLibraryFilterToggled) },
                        )
                    }
                },
            )
        },
        isLoading = state.isLoading,
        error = state.error?.asString()?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(ScheduleState.Event.RetrySelected) },
        isEmpty = !state.onlyInLibrary && dayGroups.isEmpty(),
        errorContent = state.error?.asString()?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(CoreR.string.common_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        OfflineBanner(isOffline = LocalIsOffline.current)
        ScheduleMobileDateChips(
            groups = dayGroups,
            selectedEpochDay = schedule.selectedEpochDay,
            onSelected = { epochDay ->
                tabsState.selectPage(dayGroups.indexOfFirst { it.date.toEpochDay() == epochDay })
            },
            transition = tabsState::tabTransition,
        )

        if (dayGroups.isEmpty()) {
            MobileMessage(
                title = stringResource(R.string.schedule_filter_in_library_empty),
                icon = Icons.Filled.DateRange,
                actionLabel = stringResource(R.string.schedule_filter_show_all),
                onAction = { onEvent(ScheduleState.Event.InLibraryFilterToggled) },
                modifier = Modifier.weight(1f),
            )
        } else {
            MobileSwipeableTabsPager(
                state = tabsState,
                modifier = Modifier.weight(1f),
                key = { page -> dayGroups.getOrNull(page)?.date?.toEpochDay() ?: page },
            ) { page ->
                val group = dayGroups.getOrNull(page) ?: return@MobileSwipeableTabsPager
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(ScheduleState.Event.RefreshRequested) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ScheduleDayReleases(
                        group = group,
                        now = schedule.now,
                        zone = schedule.zone,
                        onAnimeSelected = { onEvent(ScheduleState.Event.AnimeSelected(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleDayReleases(
    group: ScheduleDayUi,
    now: ZonedDateTime,
    zone: ZoneId,
    onAnimeSelected: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .mobileContentMaxWidth()
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 10.dp,
            end = 16.dp,
            bottom = MobileBottomBarDefaults.contentBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(group.items, key = { it.focusKey }) { release ->
            ScheduleMobileReleaseCard(
                release = release,
                now = now,
                zone = zone,
                onClick = { onAnimeSelected(release.item.animeId) },
            )
        }
    }
}
