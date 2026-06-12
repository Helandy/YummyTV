package su.afk.yummy.tv.feature.details.screenshots

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerItem
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberFocusRestorerState
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.screenshots.view.ScreenshotCard
import su.afk.yummy.tv.feature.details.screenshots.view.ScreenshotPreview

@Composable
fun ScreenshotsTvScreen(

    state: ScreenshotsState.State,
    effect: Flow<ScreenshotsState.Effect>,
    onEvent: (ScreenshotsState.Event) -> Unit,

) {
    BackHandler { onEvent(ScreenshotsState.Event.BackSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
            state.screenshots.isEmpty() -> Text(
                text = stringResource(R.string.details_screenshots_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> {
                val restorerState = rememberFocusRestorerState()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 284.dp),
                    contentPadding = PaddingValues(
                        start = TvScreenPadding.Horizontal,
                        top = TvScreenPadding.Vertical,
                        end = TvScreenPadding.Horizontal,
                        bottom = TvScreenPadding.Vertical,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                    verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = state.title.ifBlank { stringResource(R.string.details_screenshots_title) },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    itemsIndexed(
                        items = state.screenshots,
                        key = { _, item -> item.id ?: item.hashCode() },
                    ) { index, screenshot ->
                        ScreenshotCard(
                            screenshot = screenshot,
                            onClick = { onEvent(ScreenshotsState.Event.ScreenshotSelected(index)) },
                            modifier = Modifier
                                .focusRestorerItem(index, restorerState),
                        )
                    }
                }
            }
        }

        val selectedIndex = state.selectedIndex
        if (selectedIndex != null) {
            ScreenshotPreview(
                state = state,
                index = selectedIndex,
                onPrevious = { onEvent(ScreenshotsState.Event.PreviousSelected) },
                onNext = { onEvent(ScreenshotsState.Event.NextSelected) },
            )
        }
    }
}
