package su.afk.yummy.tv.feature.schedule.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.schedule.ScheduleState
import su.afk.yummy.tv.feature.schedule.mobile.view.ScheduleMobileDateChips
import su.afk.yummy.tv.feature.schedule.mobile.view.ScheduleMobileReleaseCard
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi
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
            error = "Не удалось загрузить расписание"
        ), emptyFlow()
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
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = dayGroups
            .indexOfFirst { it.date.toEpochDay() == schedule.selectedEpochDay }
            .coerceAtLeast(0),
        pageCount = dayGroups.size.coerceAtLeast(1),
        onPageSelected = { page ->
            dayGroups.getOrNull(page)?.let {
                onEvent(ScheduleState.Event.DateSelected(it.date.toEpochDay()))
            }
        },
    )

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.schedule_mobile_title),
                onBack = { onEvent(ScheduleState.Event.BackSelected) },
            )
        },
        isLoading = state.isLoading,
        error = state.error?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(ScheduleState.Event.RetrySelected) },
        isEmpty = schedule.dayGroups.isEmpty(),
        errorContent = state.error?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.schedule_mobile_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        ScheduleMobileDateChips(
            groups = dayGroups,
            selectedEpochDay = schedule.selectedEpochDay,
            onSelected = { epochDay ->
                tabsState.selectPage(dayGroups.indexOfFirst { it.date.toEpochDay() == epochDay })
            },
        )

        if (dayGroups.isNotEmpty()) {
            MobileSwipeableTabsPager(
                state = tabsState,
                modifier = Modifier.weight(1f),
                key = { page -> dayGroups[page].date.toEpochDay() },
            ) { page ->
                ScheduleDayReleases(
                    group = dayGroups[page],
                    now = schedule.now,
                    zone = schedule.zone,
                    onAnimeSelected = { onEvent(ScheduleState.Event.AnimeSelected(it)) },
                )
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
