package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal const val PLAYER_INLINE_TOAST_DURATION_MS = 2_000L

@Composable
internal fun PlayerInlineToast(
    text: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = text.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
    }
}
