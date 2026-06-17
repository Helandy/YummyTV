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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.schedule.mobile.R
import su.afk.yummy.tv.feature.schedule.view.ScheduleMobileDateChips
import su.afk.yummy.tv.feature.schedule.view.ScheduleMobileReleaseCard

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
