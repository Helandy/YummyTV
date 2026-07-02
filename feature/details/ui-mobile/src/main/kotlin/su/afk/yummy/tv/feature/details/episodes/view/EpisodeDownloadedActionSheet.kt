package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.R
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EpisodeDownloadedActionSheet(
    action: EpisodesState.DownloadedEpisodeAction,
    onPlay: () -> Unit,
    onRedownloadDubbing: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.details_mobile_downloaded_episode_actions_title,
                    action.episode,
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = action.downloadedDubbing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            )
            Text(
                text = stringResource(
                    R.string.details_mobile_downloaded_episode_player_quality,
                    action.playerName.playerLabel(),
                    action.qualityLabel,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            )
            action.bytesDownloaded.formatMegabytesOrNull()?.let { size ->
                Text(
                    text = stringResource(
                        R.string.details_mobile_downloaded_episode_disk_size,
                        size
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                )
            }
            TextButton(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.details_mobile_play_downloaded_episode),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (action.hasAlternativeDubbings) {
                TextButton(onClick = onRedownloadDubbing, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.details_mobile_redownload_dubbing),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.details_mobile_download_other_dubbing_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

private fun String.playerLabel(): String =
    trim()
        .removePrefix("Плеер ")
        .removePrefix("Player ")

private fun Long.formatMegabytesOrNull(): String? {
    if (this <= 0L) return null
    val megabytes = toDouble() / (1024.0 * 1024.0)
    return if (megabytes < 100.0) {
        String.format(Locale.US, "%.1f MB", megabytes)
    } else {
        String.format(Locale.US, "%.0f MB", megabytes)
    }
}
