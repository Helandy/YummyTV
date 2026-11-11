package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Центральный кластер как у YouTube: «предыдущая серия», крупный play/pause и «следующая серия»
 * поверх видео. Смена серии срабатывает сразу по тапу.
 */
@Composable
internal fun MobilePlayerCenterControls(
    wantsPlay: Boolean,
    hasPrevEpisode: Boolean,
    onPlayPause: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    /** Идёт буферизация: поверх кнопки крутится индикатор, значок play/pause прячем. */
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CENTER_CONTROLS_GAP),
    ) {
        MobilePlayerActionButton(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = stringResource(R.string.player_mobile_previous_episode),
            enabled = hasPrevEpisode,
            onClick = onPrevEpisode,
            sizeMultiplier = CENTER_SWITCH_SCALE,
        )
        val playPauseDescription = stringResource(
            if (wantsPlay) R.string.player_mobile_pip_pause else R.string.player_mobile_pip_play,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CENTER_PLAY_SIZE)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = playPauseDescription,
                    onClick = onPlayPause,
                ),
        ) {
            if (!loading) {
                Icon(
                    imageVector = if (wantsPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = playPauseDescription,
                    tint = Color.White,
                    modifier = Modifier.size(CENTER_PLAY_ICON_SIZE),
                )
            }
        }
        MobilePlayerActionButton(
            icon = Icons.Filled.SkipNext,
            contentDescription = stringResource(R.string.player_mobile_next_episode),
            onClick = onNextEpisode,
            sizeMultiplier = CENTER_SWITCH_SCALE,
        )
    }
}

/** Кнопки смены серии крупнее обычных на 10%, чтобы не терялись рядом с play/pause. */
private const val CENTER_SWITCH_SCALE = 1.1f
private val CENTER_CONTROLS_GAP = 40.dp
private val CENTER_PLAY_SIZE = 72.dp
private val CENTER_PLAY_ICON_SIZE = 44.dp
