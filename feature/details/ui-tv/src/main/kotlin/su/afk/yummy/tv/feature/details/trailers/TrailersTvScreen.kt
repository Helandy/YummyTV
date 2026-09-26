package su.afk.yummy.tv.feature.details.trailers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.focus.rememberTvGridStartExtent
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.trailers.view.TrailerCard

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
@Composable
private fun TrailersTvScreenDefaultPreview() = ScreenPreviewTheme {
    TrailersTvScreen(TrailersState.State(isLoading = false), emptyFlow()) {}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrailersTvScreen(
    state: TrailersState.State,
    effect: Flow<TrailersState.Effect>,
    onEvent: (TrailersState.Event) -> Unit,
) {
    BackHandler { onEvent(TrailersState.Event.BackSelected) }

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
                onRetry = { onEvent(TrailersState.Event.RetrySelected) },
            )

            state.trailers.isEmpty() -> TvStateMessage(
                title = stringResource(R.string.details_trailer_empty),
                icon = Icons.Outlined.Movie,
            )

            else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val context = LocalContext.current
                val gridState = rememberLazyGridState()
                val scope = rememberCoroutineScope()
                val trailerCount = state.trailers.size
                val focusRequesters =
                    remember(trailerCount) { List(trailerCount) { FocusRequester() } }
                val horizontalSpacing = TvCardSpacing.Horizontal
                val gridColumnCount =
                    (
                        ((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                            (TrailerCardMinWidth.value + horizontalSpacing.value)
                        ).toInt()
                        .coerceAtLeast(1)
                val gridStartExtent =
                    rememberTvGridStartExtent(TvScreenPadding.Vertical, TvCardSpacing.Vertical)

                CompositionLocalProvider(
                    LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(TvCardSpacing.Vertical),
                ) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = TrailerCardMinWidth),
                        contentPadding = PaddingValues(
                            start = TvScreenPadding.Horizontal,
                            top = TvScreenPadding.Vertical,
                            end = TvScreenPadding.Horizontal,
                            bottom = TvScreenPadding.Vertical,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                        verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                        modifier = Modifier
                            .fillMaxSize()
                            .tvFocusRestorer(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = { "header" }) {
                            Text(
                                text = stringResource(R.string.details_trailers),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = gridStartExtent.measure.padding(bottom = 2.dp),
                            )
                        }
                        itemsIndexed(
                            items = state.trailers,
                            key = { _, trailer -> trailer.iframeUrl },
                            contentType = { _, _ -> "trailer" },
                        ) { index, trailer ->
                            TrailerCard(
                                number = index + 1,
                                thumbnailUrl = trailer.youtubeThumbnailUrl,
                                onClick = { context.openExternalUri(trailer.externalWatchUrl) },
                                modifier = Modifier
                                    .focusRequester(focusRequesters[index])
                                    .tvWholeItemBringIntoView(gridStartExtent.takeIf { index < gridColumnCount })
                                    .tvLazyGridRowFocusNavigation(
                                        index = index,
                                        columnCount = gridColumnCount,
                                        itemCount = trailerCount,
                                        gridState = gridState,
                                        scope = scope,
                                        focusRequesterAt = focusRequesters::getOrNull,
                                        // нулевой lazy-индекс занимает заголовок
                                        lazyIndexOffset = 1,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val TrailerCardMinWidth = 220.dp
