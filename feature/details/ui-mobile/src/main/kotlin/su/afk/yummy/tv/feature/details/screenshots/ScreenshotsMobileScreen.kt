package su.afk.yummy.tv.feature.details.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScreenshotsMobileScreen(
    state: ScreenshotsState.State,
    effect: Flow<ScreenshotsState.Effect>,
    onEvent: (ScreenshotsState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = state.title.ifBlank { stringResource(R.string.details_mobile_screenshots) },
                onBack = { onEvent(ScreenshotsState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            empty = state.screenshots.isEmpty() && !state.isLoading,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = state.screenshots,
                    key = { _, screenshot -> screenshot.id ?: screenshot.hashCode() },
                ) { index, screenshot ->
                    ScreenshotMobileCard(
                        screenshot = screenshot,
                        onClick = { onEvent(ScreenshotsState.Event.ScreenshotSelected(index)) },
                    )
                }
            }
        }
    }
    state.selectedIndex?.let { index ->
        val screenshot = state.screenshots.getOrNull(index)
        if (screenshot != null) {
            ScreenshotFullscreenDialog(
                screenshot = screenshot,
                onDismiss = { onEvent(ScreenshotsState.Event.ScreenshotDismissed) },
            )
        }
    }
}

@Composable
private fun ScreenshotMobileCard(
    screenshot: AnimeScreenshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = screenshot.small ?: screenshot.full,
            contentDescription = screenshot.episode?.let {
                stringResource(R.string.details_mobile_episode, it)
            } ?: stringResource(R.string.details_mobile_screenshot),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
    }
}

@Composable
private fun ScreenshotFullscreenDialog(
    screenshot: AnimeScreenshot,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = screenshot.full ?: screenshot.small,
                contentDescription = screenshot.episode?.let {
                    stringResource(R.string.details_mobile_episode, it)
                } ?: stringResource(R.string.details_mobile_screenshot),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.details_mobile_close),
                    tint = Color.White,
                )
            }
        }
    }
}
