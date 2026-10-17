package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Переключатель вкл/выкл для строк меню настроек ТВ-плеера. Состояние читается без текста:
 * включённый — акцентного цвета с ползунком справа, выключенный — тусклый с ползунком слева.
 */
@Composable
internal fun TvPlayerSettingsToggle(
    checked: Boolean,
    modifier: Modifier = Modifier,
    /** Строка в фокусе: она сама залита акцентным цветом, поэтому включённый переключатель инвертируется. */
    focused: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    val track by animateColorAsState(
        targetValue = when {
            checked && focused -> Color.White
            checked -> accent
            else -> Color.Black.copy(alpha = 0.45f)
        },
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvSettingsToggleTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvSettingsToggleThumb",
    )
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 26.dp)
            .background(track, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .background(
                    when {
                        checked && focused -> accent
                        checked -> Color.White
                        else -> Color.White.copy(alpha = 0.75f)
                    },
                    CircleShape,
                ),
        )
    }
}
