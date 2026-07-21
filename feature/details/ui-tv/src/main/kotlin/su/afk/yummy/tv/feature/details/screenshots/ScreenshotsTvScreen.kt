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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.screenshots.utils.screenshotLazyKey
import su.afk.yummy.tv.feature.details.screenshots.view.ScreenshotCard
import su.afk.yummy.tv.feature.details.screenshots.view.ScreenshotPreview

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun ScreenshotsTvScreenDefaultPreview() = ScreenPreviewTheme {
    ScreenshotsTvScreen(ScreenshotsState.State(isLoading = false), emptyFlow()) {}
}

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
            state.isLoading -> TvLoadingScreen()

            state.error != null -> TvStateMessage(
                title = state.error.orEmpty(),
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(ScreenshotsState.Event.RetrySelected) },
            )

            state.screenshots.isEmpty() -> TvStateMessage(
                title = stringResource(R.string.details_screenshots_empty),
                icon = Icons.Outlined.Image,
            )

            else -> {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .tvFocusRestorer(),
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
                        key = { index, item -> item.screenshotLazyKey(index) },
                    ) { index, screenshot ->
                        ScreenshotCard(
                            screenshot = screenshot,
                            onClick = { onEvent(ScreenshotsState.Event.ScreenshotSelected(index)) },
                            modifier = Modifier,
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
