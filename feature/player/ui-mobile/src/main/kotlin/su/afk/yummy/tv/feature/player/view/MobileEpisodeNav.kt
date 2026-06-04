package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.R as UiR

@Composable
internal fun MobileEpisodeNav(
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasPrevEpisode && !hasNextEpisode) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledTonalButton(
            enabled = hasPrevEpisode,
            onClick = onPrevEpisode,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = null)
            Spacer(Modifier.widthIn(min = 8.dp))
            Text(stringResource(UiR.string.player_mobile_previous_episode), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
            enabled = hasNextEpisode,
            onClick = onNextEpisode,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(UiR.string.player_mobile_next_episode), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.widthIn(min = 8.dp))
            Icon(Icons.Filled.SkipNext, contentDescription = null)
        }
    }
}
