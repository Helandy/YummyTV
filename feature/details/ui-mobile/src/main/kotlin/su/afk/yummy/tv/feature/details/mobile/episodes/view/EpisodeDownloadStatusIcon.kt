package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.isDownloadBusy

private val StatusIconSize = 22.dp

@Composable
internal fun EpisodeDownloadStatusIcon(
    status: EpisodesState.EpisodeDownloadUiState?,
    resolving: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    when {
        resolving -> StatusIcon(Icons.Filled.HourglassEmpty, colors.tertiary)

        status.isDownloadBusy() -> {
            val modifier = Modifier.size(StatusIconSize)
            if (status?.status == EpisodesState.EpisodeDownloadUiStatus.Queued) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = modifier)
            } else {
                CircularProgressIndicator(
                    progress = { status?.progress ?: 0f },
                    strokeWidth = 2.dp,
                    modifier = modifier,
                )
            }
        }

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded ->
            StatusIcon(Icons.Filled.Storage, colors.primary)

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Paused ->
            StatusIcon(Icons.Filled.PauseCircleOutline, colors.onSurfaceVariant)

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Failed ->
            StatusIcon(Icons.Filled.ErrorOutline, colors.error)

        else -> StatusIcon(Icons.Filled.Download, colors.onSurfaceVariant)
    }
}

@Composable
private fun StatusIcon(imageVector: ImageVector, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(StatusIconSize),
    )
}
