package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun MobilePlayerActionButton(
    icon: ImageVector,
    /** Подпись для TalkBack: у кнопки нет текста, иначе она объявляется просто «кнопкой». */
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    /**
     * Режим удержания, как у разблокировки: действие срабатывает только после [HOLD_TO_CONFIRM_MS]
     * с кольцом прогресса, короткий тап лишь вызывает [onHoldHint]. [onClick] в этом режиме не используется.
     */
    onHold: (() -> Unit)? = null,
    onHoldHint: () -> Unit = {},
) {
    val shape = CircleShape
    val background = when {
        !enabled -> Color.White.copy(alpha = 0.06f)
        primary -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.14f)
    }
    val contentColor = when {
        !enabled -> Color.White.copy(alpha = 0.26f)
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> Color.White
    }
    val borderColor =
        if (primary) Color.Transparent else Color.White.copy(alpha = 0.18f)
    val holdProgress = remember { Animatable(0f) }
    val buttonSize = actionButtonSize(primary)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .then(
                if (onHold == null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = contentDescription,
                        onClick = onClick,
                    )
                } else {
                    Modifier.holdToConfirm(
                        enabled = enabled,
                        progress = holdProgress,
                        contentDescription = contentDescription,
                        onHold = onHold,
                        onHoldHint = onHoldHint,
                    )
                },
            ),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = contentColor)
        if (onHold != null) {
            CircularProgressIndicator(
                progress = { holdProgress.value },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                strokeWidth = 3.dp,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Размер кнопки подбирается по ширине экрана: ряд из семи кнопок в портрете на узких телефонах
 * (360dp) иначе не помещается, а на широких экранах и в ландшафте нет причин мельчить.
 */
@Composable
private fun actionButtonSize(primary: Boolean): Dp {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= WIDE_SCREEN_DP -> if (primary) 64.dp else 48.dp
        widthDp >= MEDIUM_SCREEN_DP -> if (primary) 60.dp else 44.dp
        else -> if (primary) 56.dp else 40.dp
    }
}

private const val MEDIUM_SCREEN_DP = 392
private const val WIDE_SCREEN_DP = 440

/** Общая длительность удержания для подтверждения действия (разблокировка, смена серии). */
internal const val HOLD_TO_CONFIRM_MS = 600

private fun Modifier.holdToConfirm(
    enabled: Boolean,
    progress: Animatable<Float, AnimationVector1D>,
    contentDescription: String,
    onHold: () -> Unit,
    onHoldHint: () -> Unit,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentOnHold by rememberUpdatedState(onHold)
    val currentOnHoldHint by rememberUpdatedState(onHoldHint)
    this
        .semantics {
            role = Role.Button
            this.contentDescription = contentDescription
            // TalkBack не умеет удерживать: двойной тап выполняет действие сразу.
            if (enabled) {
                onClick {
                    currentOnHold()
                    true
                }
            }
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    val hold = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(HOLD_TO_CONFIRM_MS, easing = LinearEasing))
                        currentOnHold()
                        progress.snapTo(0f)
                    }
                    val released = tryAwaitRelease()
                    if (hold.isActive) {
                        hold.cancel()
                        scope.launch { progress.snapTo(0f) }
                        if (released) currentOnHoldHint()
                    }
                },
            )
        }
}
