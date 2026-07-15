package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
internal fun TvPlayerEpisodeRow(
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    canRateTitle: Boolean,
    qualityCount: Int,
    allDubbingNames: List<String>,
    currentDubbingIndex: Int,
    allBalancerNames: List<String>,
    currentBalancerIndex: Int,
    playerName: String,
    dubbing: String,
    currentQualityLabel: String,
    currentSpeedLabel: String,
    qualityFocusRequester: FocusRequester? = null,
    dubbingFocusRequester: FocusRequester? = null,
    balancerFocusRequester: FocusRequester? = null,
    resizeFocusRequester: FocusRequester? = null,
    speedFocusRequester: FocusRequester? = null,
    onInteraction: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onRateTitle: () -> Unit,
    onToggleQuality: () -> Unit,
    onToggleDubbing: () -> Unit,
    onToggleBalancer: () -> Unit,
    onToggleResize: () -> Unit,
    onToggleSpeed: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPrevEpisode) {
            TvControlButton(onClick = onPrevEpisode, onFocused = onInteraction) { color ->
                Text(stringResource(R.string.player_previous_episode), style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
        Spacer(Modifier.weight(1f))
        if (allDubbingNames.size > 1) {
            val label = allDubbingNames
                .getOrElse(currentDubbingIndex) { dubbing }
                .withoutDubbingTitlePrefix(stringResource(R.string.player_dubbing_title))
            TvControlButton(
                onClick = onToggleDubbing,
                onFocused = onInteraction,
                focusRequester = dubbingFocusRequester,
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
            Spacer(Modifier.width(8.dp))
        }
        if (allBalancerNames.size > 1) {
            val label = allBalancerNames.getOrElse(currentBalancerIndex) { playerName }
                .removePrefix(stringResource(R.string.player_name_prefix))
            TvControlButton(
                onClick = onToggleBalancer,
                onFocused = onInteraction,
                focusRequester = balancerFocusRequester,
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
            Spacer(Modifier.width(8.dp))
        }
        if (qualityCount > 0) {
            TvControlButton(
                onClick = onToggleQuality,
                onFocused = onInteraction,
                focusRequester = qualityFocusRequester,
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
            Spacer(Modifier.width(8.dp))
        }
        TvControlButton(
            onClick = onToggleResize,
            onFocused = onInteraction,
            focusRequester = resizeFocusRequester,
            modifier = Modifier.width(48.dp),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 9.dp),
        ) { color ->
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.player_resize_title),
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        TvControlButton(
            onClick = onToggleSpeed,
            onFocused = onInteraction,
            focusRequester = speedFocusRequester,
            modifier = Modifier.widthIn(min = 76.dp),
        ) { color ->
            Text(
                text = currentSpeedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hasNextEpisode || canRateTitle) {
            Spacer(Modifier.width(8.dp))
        }
        if (hasNextEpisode) {
            TvControlButton(onClick = onNextEpisode, onFocused = onInteraction) { color ->
                Text(stringResource(R.string.player_next_episode), style = MaterialTheme.typography.labelLarge, color = color)
            }
        } else if (canRateTitle) {
            TvControlButton(onClick = onRateTitle, onFocused = onInteraction) { color ->
                Text(stringResource(R.string.player_rate_title), style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
    }
}

private fun String.withoutDubbingTitlePrefix(title: String): String {
    val trimmed = trim()
    if (!trimmed.startsWith(title, ignoreCase = true)) return trimmed

    val withoutTitle = trimmed
        .drop(title.length)
        .trimStart()
        .trimStart(':', '-')
        .trimStart()

    return withoutTitle.ifBlank { trimmed }
}
