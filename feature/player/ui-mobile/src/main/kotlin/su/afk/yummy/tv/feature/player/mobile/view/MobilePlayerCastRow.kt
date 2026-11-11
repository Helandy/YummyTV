package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.cast.MediaRouteButton
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Строка Cast: настоящая невидимая [MediaRouteButton] растянута на всю строку - только она умеет
 * открывать системный выбор устройства, а ripple рисуется отдельно (alpha скрыл бы и его).
 */
@Composable
internal fun MobilePlayerCastRow() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            // Заливка при нажатии, но без своего clickable: тап должен дойти до кнопки ниже.
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Системный Output Switcher уводит Activity из foreground - без подсказки
                    // onUserLeaveHint() затянул бы в PiP посреди выбора устройства.
                    MobilePlayerPipController.suppressAutoEnterForCastPicker()
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    val up = waitForUpOrCancellation()
                    interactionSource.tryEmit(
                        if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                    )
                }
            },
    ) {
        MediaRouteButton(
            modifier = Modifier
                .matchParentSize()
                .alpha(0f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Cast, contentDescription = null)
            }
            Text(
                text = stringResource(R.string.player_mobile_cast),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
