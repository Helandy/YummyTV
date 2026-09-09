package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.cast.MediaRouteButton
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.designsystem.baseScreen.HideSheetWindowSystemBars
import su.afk.yummy.tv.feature.player.mobile.R
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerMobileActionsSheet(
    showDetails: Boolean,
    showPictureInPicture: Boolean,
    showCast: Boolean,
    onDetails: () -> Unit,
    onPictureInPicture: () -> Unit,
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(onDismissRequest = onDismiss, scrollableContent = true) {
        HideSheetWindowSystemBars()

        if (showDetails) {
            ActionRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.player_mobile_details),
                onClick = {
                    onDismiss()
                    onDetails()
                },
            )
        }
        if (showCast) {
            val shape = RoundedCornerShape(8.dp)
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    // Заливка при нажатии, как у Details/PiP - но без своего clickable: тап не
                    // должен потребляться здесь, иначе он не дойдёт до настоящей кнопки ниже.
                    .indication(interactionSource, LocalIndication.current)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Открытие системного Output Switcher уводит Activity из foreground -
                            // без этой подсказки onUserLeaveHint() затянул бы в PiP посреди выбора
                            // устройства (см. MobilePlayerPipController.suppressAutoEnterForCastPicker).
                            MobilePlayerPipController.suppressAutoEnterForCastPicker()
                            val press = PressInteraction.Press(down.position)
                            interactionSource.tryEmit(press)
                            val up = waitForUpOrCancellation()
                            interactionSource.tryEmit(
                                if (up != null) {
                                    PressInteraction.Release(press)
                                } else {
                                    PressInteraction.Cancel(press)
                                }
                            )
                        }
                    },
            ) {
                // Настоящая кнопка растянута на всю строку, но невидима (alpha=0) - она одна
                // умеет открывать системный диалог выбора Cast-устройства (MediaRouteButtonState
                // .isPickerVisible - internal setter, снаружи библиотеки недоступен), поэтому её
                // область клика должна покрывать всю строку, а не только 48dp иконки. Ripple выше
                // рисуется отдельно (см. pointerInput) - alpha скрывает и её тоже, если рисовать
                // внутри самой кнопки.
                MediaRouteButton(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Cast, contentDescription = null)
                    }
                    Text(
                        text = stringResource(R.string.player_mobile_cast),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        if (showPictureInPicture) {
            ActionRow(
                icon = Icons.Filled.PictureInPictureAlt,
                label = stringResource(R.string.player_mobile_pip),
                onClick = {
                    onDismiss()
                    onPictureInPicture()
                },
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 48dp-бокс, чтобы левый край иконки совпадал с MediaRouteButton в строке Cast
        // (он сам оборачивает свою иконку в 48dp IconButton).
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null)
        }
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
