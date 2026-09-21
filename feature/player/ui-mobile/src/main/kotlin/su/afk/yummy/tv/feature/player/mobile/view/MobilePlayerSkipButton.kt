package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip
import su.afk.yummy.tv.feature.player.common.view.autoSkipProgressFill
import su.afk.yummy.tv.feature.player.model.PlayerSkipType
import su.afk.yummy.tv.feature.player.mobile.R as UiR

/**
 * Плавающая кнопка ручного пропуска опенинга/эндинга: живёт весь сегмент
 * и не зависит от авто-скрытия панели управления.
 */
@Composable
internal fun MobilePlayerSkipButton(
    skip: PlayerActiveSkip?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    countdownSeconds: Int? = null,
    countdownProgress: Float? = null,
) {
    // Тип держим отдельно, чтобы подпись не менялась во время анимации исчезновения.
    var lastType by remember { mutableStateOf(PlayerSkipType.Opening) }
    skip?.type?.let { lastType = it }
    val shape = RoundedCornerShape(12.dp)

    AnimatedVisibility(
        visible = skip != null,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(YummySemanticColors.PanelScrim)
                .autoSkipProgressFill(countdownProgress, Color.White.copy(alpha = 0.22f), 12.dp)
                .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = if (countdownSeconds != null) {
                    stringResource(
                        when (lastType) {
                            PlayerSkipType.Opening -> UiR.string.player_mobile_skip_opening_countdown
                            PlayerSkipType.Ending -> UiR.string.player_mobile_skip_ending_countdown
                        },
                        countdownSeconds,
                    )
                } else {
                    stringResource(
                        when (lastType) {
                            PlayerSkipType.Opening -> UiR.string.player_mobile_skip_opening
                            PlayerSkipType.Ending -> UiR.string.player_mobile_skip_ending
                        }
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
    }
}
