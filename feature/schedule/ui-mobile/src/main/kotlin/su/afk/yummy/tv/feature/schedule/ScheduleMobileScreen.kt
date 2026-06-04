package su.afk.yummy.tv.feature.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.schedule.mobile.R
import su.afk.yummy.tv.feature.schedule.utils.formatAirTime

@Composable
fun ScheduleMobileScreen(

    state: ScheduleState.State,
    effect: Flow<ScheduleState.Effect>,
    onEvent: (ScheduleState.Event) -> Unit,

) {
    MobileStateContent(
        isLoading = state.isLoading,
        error = state.error,
        onRetry = { onEvent(ScheduleState.Event.RetrySelected) },
        empty = state.days.isEmpty(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.days.forEach { day ->
                item { Text(day.title, style = MaterialTheme.typography.titleMedium) }
                items(day.items, key = { it.animeId }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        subtitle = item.nextDateEpochSeconds?.formatAirTime()
                            ?: item.airedEpisodes?.let { stringResource(R.string.schedule_mobile_aired_episodes, it) },
                        onClick = { onEvent(ScheduleState.Event.AnimeSelected(item.animeId)) },
                    )
                }
            }
        }
    }
}
