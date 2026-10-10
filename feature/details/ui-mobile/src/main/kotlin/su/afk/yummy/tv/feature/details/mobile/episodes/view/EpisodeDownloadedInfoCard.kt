package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.formatMegabytesOrNull
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.playerLabel
import su.afk.yummy.tv.feature.details.presentation.R

/** Сводка по скачанной серии: озвучка, плеер и качество, занятое место. */
@Composable
internal fun EpisodeDownloadedInfoCard(action: EpisodesState.DownloadedEpisodeAction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoLine(Icons.Filled.VideoLibrary, action.downloadedDubbing)
        InfoLine(
            Icons.Filled.Tune,
            stringResource(
                R.string.details_mobile_downloaded_episode_player_quality,
                action.playerName.playerLabel(),
                action.qualityLabel,
            ),
        )
        action.bytesDownloaded.formatMegabytesOrNull()?.let { size ->
            InfoLine(
                Icons.Filled.Storage,
                stringResource(R.string.details_mobile_downloaded_episode_disk_size, size),
            )
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
