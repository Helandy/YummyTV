package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.episodes.utils.isDownloadBusy

private val DownloadResolvingColor = Color(0xFFFFC107)

@Composable
internal fun EpisodeDownloadStatusIcon(
    status: EpisodesState.EpisodeDownloadUiState?,
    resolving: Boolean,
) {
    when {
        resolving -> Icon(
            imageVector = Icons.Filled.HourglassEmpty,
            contentDescription = null,
            tint = DownloadResolvingColor,
        )

        status.isDownloadBusy() -> CircularProgressIndicator(
            progress = { status?.progress ?: 0f },
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp),
        )

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded ->
            Icon(Icons.Filled.Storage, contentDescription = null)

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Failed ->
            Icon(Icons.Filled.ErrorOutline, contentDescription = null)

        else -> Icon(Icons.Filled.Download, contentDescription = null)
    }
}
