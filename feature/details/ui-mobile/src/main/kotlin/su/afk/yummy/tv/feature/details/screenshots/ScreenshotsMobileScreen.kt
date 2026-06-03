package su.afk.yummy.tv.feature.details.screenshots

import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold

@Composable
fun ScreenshotsMobileScreen(
    state: ScreenshotsState.State,
    effect: Flow<ScreenshotsState.Effect>,
    onEvent: (ScreenshotsState.Event) -> Unit,
) {
    DetailsMobileScaffold(title = state.title.ifBlank { "Скриншоты" }, onBack = { onEvent(ScreenshotsState.Event.BackSelected) }) { padding ->
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            empty = state.screenshots.isEmpty() && !state.isLoading,
        ) {
            MobilePosterGrid(contentPadding = padding) {
                itemsIndexed(state.screenshots) { index, screenshot ->
                    MobilePosterCard(
                        title = screenshot.episode?.let { "Эпизод $it" } ?: "Скриншот",
                        posterUrl = screenshot.small ?: screenshot.full,
                        onClick = { onEvent(ScreenshotsState.Event.ScreenshotSelected(index)) },
                    )
                }
            }
        }
    }
    state.selectedIndex?.let { index ->
        val screenshot = state.screenshots.getOrNull(index)
        if (screenshot != null) {
            AlertDialog(
                onDismissRequest = { onEvent(ScreenshotsState.Event.ScreenshotDismissed) },
                confirmButton = {
                    Button(onClick = { onEvent(ScreenshotsState.Event.ScreenshotDismissed) }) { Text("Закрыть") }
                },
                text = {
                    AsyncImage(model = screenshot.full ?: screenshot.small, contentDescription = null)
                },
            )
        }
    }
}
