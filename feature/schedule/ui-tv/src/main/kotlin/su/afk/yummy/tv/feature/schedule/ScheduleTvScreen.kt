package su.afk.yummy.tv.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.schedule.AnimeScheduleItem

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
            state.isLoading -> Text(stringResource(R.string.schedule_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.error != null -> Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            state.days.isEmpty() -> Text(stringResource(R.string.schedule_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(TvScreenPadding.Horizontal, TvScreenPadding.Vertical),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                items(state.days) { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = day.title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            itemsIndexed(day.items, key = { _, item -> item.animeId }) { _, item ->
                                ScheduleCard(item) { onEvent(ScheduleState.Event.AnimeSelected(item.animeId)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(item: AnimeScheduleItem, onClick: () -> Unit) {
    TvTitleCard(
        title = item.title,
        posterUrl = item.posterUrl,
        onClick = onClick,
        onFocused = {},
        subtitle = listOfNotNull(
            item.airedEpisodes?.let { stringResource(R.string.schedule_aired, it) },
            item.totalEpisodes?.let { stringResource(R.string.schedule_total, it) },
        ).joinToString(" / "),
    )
}
