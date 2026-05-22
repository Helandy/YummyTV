package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.R

@Composable
internal fun PlayerEpisodeRow(
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    qualityCount: Int,
    allDubbingNames: List<String>,
    currentDubbingIndex: Int,
    allBalancerNames: List<String>,
    currentBalancerIndex: Int,
    playerName: String,
    dubbing: String,
    currentQualityLabel: String,
    onInteraction: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onToggleQuality: () -> Unit,
    onToggleDubbing: () -> Unit,
    onToggleBalancer: () -> Unit,
) {
    val showRow = hasPrevEpisode || hasNextEpisode || qualityCount > 1 ||
                  allDubbingNames.size > 1 || allBalancerNames.size > 1
    if (!showRow) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPrevEpisode) {
            ControlButton(onClick = onPrevEpisode, onFocused = onInteraction) { color ->
                Text(stringResource(R.string.player_previous_episode), style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
        Spacer(Modifier.weight(1f))
        if (allBalancerNames.size > 1) {
            val label = allBalancerNames.getOrElse(currentBalancerIndex) { playerName }
                .removePrefix(stringResource(R.string.player_name_prefix))
            ControlButton(
                onClick = onToggleBalancer,
                onFocused = onInteraction,
                modifier = Modifier.widthIn(min = 128.dp, max = 156.dp),
            ) { color ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (allDubbingNames.size > 1 || qualityCount > 1 || hasNextEpisode) Spacer(Modifier.width(8.dp))
        }
        if (allDubbingNames.size > 1) {
            val label = allDubbingNames.getOrElse(currentDubbingIndex) { dubbing }
            ControlButton(
                onClick = onToggleDubbing,
                onFocused = onInteraction,
                modifier = Modifier.widthIn(min = 164.dp, max = 220.dp),
            ) { color ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (qualityCount > 1 || hasNextEpisode) Spacer(Modifier.width(8.dp))
        }
        if (qualityCount > 1) {
            ControlButton(
                onClick = onToggleQuality,
                onFocused = onInteraction,
                modifier = Modifier.widthIn(min = 92.dp),
            ) { color ->
                Text(
                    text = currentQualityLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hasNextEpisode) Spacer(Modifier.width(8.dp))
        }
        if (hasNextEpisode) {
            ControlButton(onClick = onNextEpisode, onFocused = onInteraction) { color ->
                Text(stringResource(R.string.player_next_episode), style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
    }
}
