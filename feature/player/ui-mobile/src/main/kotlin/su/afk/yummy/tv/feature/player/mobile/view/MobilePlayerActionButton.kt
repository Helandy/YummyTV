package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun MobilePlayerActionButton(
    icon: ImageVector,
    /** Подпись для TalkBack: у кнопки нет текста, иначе она объявляется просто «кнопкой». */
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    /** Во сколько раз увеличить кнопку и значок относительно стандартного размера. */
    sizeMultiplier: Float = 1f,
    /** Точный размер кнопки (значок масштабируется пропорционально); вместо подбора по ширине экрана. */
    size: Dp? = null,
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
    val buttonSize = size ?: (actionButtonSize(primary) * sizeMultiplier)
    val iconSize = if (size != null) {
        DEFAULT_ICON_SIZE * (size.value / REFERENCE_BUTTON_DP)
    } else {
        DEFAULT_ICON_SIZE * sizeMultiplier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
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

private val DEFAULT_ICON_SIZE = 24.dp

/** Размер кнопки (dp), при котором значок равен [DEFAULT_ICON_SIZE]. */
private const val REFERENCE_BUTTON_DP = 44f
private const val MEDIUM_SCREEN_DP = 392
private const val WIDE_SCREEN_DP = 440

/** Длительность удержания для подтверждения разблокировки экрана. */
internal const val HOLD_TO_CONFIRM_MS = 350
