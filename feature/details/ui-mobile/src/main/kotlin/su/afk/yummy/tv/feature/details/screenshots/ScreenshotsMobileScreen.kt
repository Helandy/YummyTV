package su.afk.yummy.tv.feature.details.screenshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.screenshots.view.ScreenshotFullscreenDialog
import su.afk.yummy.tv.feature.details.screenshots.view.ScreenshotMobileCard

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
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
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
                    key = { index, screenshot -> screenshot.screenshotLazyKey(index) },
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

private fun AnimeScreenshot.screenshotLazyKey(index: Int): String =
    full?.takeIf { it.isNotBlank() }
        ?: small?.takeIf { it.isNotBlank() }
        ?: "empty:$index"
