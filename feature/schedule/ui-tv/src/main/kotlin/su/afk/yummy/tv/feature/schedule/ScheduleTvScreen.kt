package su.afk.yummy.tv.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.feature.schedule.view.ScheduleLoadingState
import su.afk.yummy.tv.feature.schedule.view.ScheduleTimeline

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun ScheduleTvScreenDefaultPreview() = ScreenPreviewTheme {
    ScheduleTvScreen(ScheduleState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun ScheduleTvScreenLoadingPreview() = ScreenPreviewTheme {
    ScheduleTvScreen(ScheduleState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun ScheduleTvScreenErrorPreview() = ScreenPreviewTheme {
    ScheduleTvScreen(
        ScheduleState.State(
            isLoading = false,
            error = "Не удалось загрузить расписание"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
            state.error != null -> TvStateMessage(
                title = state.error.orEmpty(),
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(ScheduleState.Event.RetrySelected) },
            )

            state.tvSchedule.dayGroups.isEmpty() -> TvStateMessage(
                title = stringResource(R.string.schedule_empty),
                icon = Icons.Filled.DateRange,
            )

            else -> ScheduleTimeline(state.tvSchedule, onEvent)
        }
    }
}
