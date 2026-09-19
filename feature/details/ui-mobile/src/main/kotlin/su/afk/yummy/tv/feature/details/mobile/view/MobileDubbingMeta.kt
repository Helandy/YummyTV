package su.afk.yummy.tv.feature.details.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.mobile.utils.formatCompactCount

/** Просмотры и число серий озвучки — такой же блок, как в шторке настроек плеера. */
@Composable
internal fun MobileDubbingMeta(
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
