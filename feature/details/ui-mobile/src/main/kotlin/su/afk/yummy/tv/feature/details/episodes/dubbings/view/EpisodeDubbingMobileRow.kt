package su.afk.yummy.tv.feature.details.episodes.dubbings.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState
import su.afk.yummy.tv.feature.details.view.common.formatCompactCount

@Composable
internal fun EpisodeDubbingMobileRow(
    dubbing: EpisodeDubbingsState.DubbingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = dubbing.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        EpisodeDubbingMeta(
            views = dubbing.views,
            episodeCount = dubbing.episodeCount,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp),
        )
        if (dubbing.supportedBalancers.isNotBlank()) {
            Text(
                text = dubbing.supportedBalancers,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun EpisodeDubbingMeta(
    views: Int,
    episodeCount: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Visibility,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = views.formatCompactCount(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(start = 7.dp)
                .size(13.dp),
        )
        Text(
            text = episodeCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}
