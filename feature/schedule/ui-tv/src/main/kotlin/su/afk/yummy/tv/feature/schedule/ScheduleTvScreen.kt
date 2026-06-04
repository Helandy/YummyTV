package su.afk.yummy.tv.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.schedule.view.ScheduleLoadingState
import su.afk.yummy.tv.feature.schedule.view.ScheduleTimeline

@Composable
fun ScheduleTvScreen(
    state: ScheduleState.State,
    effect: Flow<ScheduleState.Effect>,
    onEvent: (ScheduleState.Event) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isLoading -> ScheduleLoadingState()
            state.error != null -> Text(
                text = state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
            state.tvSchedule.dayGroups.isEmpty() -> Text(
                text = stringResource(R.string.schedule_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> ScheduleTimeline(state.tvSchedule, onEvent)
        }
    }
}
