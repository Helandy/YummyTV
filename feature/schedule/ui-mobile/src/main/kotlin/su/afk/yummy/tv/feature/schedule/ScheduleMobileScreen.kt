package su.afk.yummy.tv.feature.schedule

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
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.schedule.mobile.R
import su.afk.yummy.tv.feature.schedule.view.ScheduleMobileDateChips
import su.afk.yummy.tv.feature.schedule.view.ScheduleMobileReleaseCard

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
    val selectedGroup = schedule.selectedGroup

    BaseScreen(
        isScroll = false,
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
            groups = schedule.dayGroups,
            selectedEpochDay = schedule.selectedEpochDay,
            onSelected = { onEvent(ScheduleState.Event.DateSelected(it)) },
        )

        if (selectedGroup != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 10.dp,
                    end = 16.dp,
                    bottom = MobileBottomBarDefaults.ContentBottomPadding +
                            MobileBottomBarDefaults.ExtraContentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(selectedGroup.items, key = { it.focusKey }) { release ->
                    ScheduleMobileReleaseCard(
                        release = release,
                        now = schedule.now,
                        zone = schedule.zone,
                        onClick = { onEvent(ScheduleState.Event.AnimeSelected(release.item.animeId)) },
                    )
                }
            }
        }
    }
}
