package su.afk.yummy.tv.feature.videodownload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.feature.videodownload.mobile.R
import su.afk.yummy.tv.feature.videodownload.view.VideoDownloadMobileCard

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun VideoDownloadMobileScreen(
    state: VideoDownloadState.State,
    effect: Flow<VideoDownloadState.Effect>,
    onEvent: (VideoDownloadState.Event) -> Unit,
) {
    val notificationPermissionGate = rememberNotificationPermissionGate()
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.video_download_mobile_title),
                onBack = { onEvent(VideoDownloadState.Event.BackSelected) },
            )
        },
    ) {
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.video_download_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    VideoDownloadMobileCard(
                        item = item,
                        onClick = { onEvent(VideoDownloadState.Event.ItemSelected(item.id)) },
                        onDetailsClick = { onEvent(VideoDownloadState.Event.DetailsSelected(item.animeId)) },
                        onDelete = { onEvent(VideoDownloadState.Event.DeleteSelected(item.id)) },
                        onPause = { onEvent(VideoDownloadState.Event.PauseSelected(item.id)) },
                        onResume = {
                            notificationPermissionGate {
                                onEvent(VideoDownloadState.Event.ResumeSelected(item.id))
                            }
                        },
                        onRestart = {
                            notificationPermissionGate {
                                onEvent(VideoDownloadState.Event.RestartSelected(item.id))
                            }
                        },
                    )
                }
            }
        }
    }
}
