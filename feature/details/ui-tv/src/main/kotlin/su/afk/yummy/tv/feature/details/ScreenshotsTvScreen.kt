package su.afk.yummy.tv.feature.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerItem
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberFocusRestorerState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.view.ScreenshotCard

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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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

@Composable
private fun ScreenshotPreview(
    state: ScreenshotsState.State,
    index: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val screenshots = state.screenshots
    val screenshot = screenshots.getOrNull(index)
    val arrowFocusRequester = remember { FocusRequester() }
    val hasNext = index < screenshots.lastIndex
    val hasPrevious = index > 0
    LaunchedEffect(index, hasNext, hasPrevious) {
        if (hasNext || hasPrevious) {
            arrowFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = screenshot?.full ?: screenshot?.small,
            contentDescription = screenshot?.episode?.let {
                stringResource(R.string.details_episode_content_description, it)
            },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        if (hasPrevious) {
            PreviewIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.details_previous),
                alignment = Alignment.CenterStart,
                onClick = onPrevious,
                focusRequester = if (!hasNext) arrowFocusRequester else null,
            )
        }

        if (hasNext) {
            PreviewIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.details_next),
                alignment = Alignment.CenterEnd,
                onClick = onNext,
                focusRequester = arrowFocusRequester,
            )
        }

        Text(
            text = "${index + 1} / ${screenshots.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PreviewIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    alignment: Alignment,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(alignment)
            .padding(24.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusableClick(onClick = onClick, shape = CircleShape)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}
