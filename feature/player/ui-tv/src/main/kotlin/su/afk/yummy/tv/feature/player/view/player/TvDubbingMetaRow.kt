package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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

@Composable
internal fun TvDubbingMetaRow(
    views: String,
    episodeCount: Int,
    sourceNames: String,
    contentColor: Color,
) {
    val metaColor = contentColor.copy(alpha = 0.62f)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = metaColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = views,
                style = MaterialTheme.typography.labelSmall,
                color = metaColor,
                maxLines = 1,
                modifier = Modifier.width(42.dp),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = null,
                tint = metaColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = episodeCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = metaColor,
                maxLines = 1,
                modifier = Modifier.width(24.dp),
            )
        }
        if (sourceNames.isNotBlank()) {
            Text(
                text = sourceNames,
                style = MaterialTheme.typography.labelSmall,
                color = metaColor,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
    }
}
