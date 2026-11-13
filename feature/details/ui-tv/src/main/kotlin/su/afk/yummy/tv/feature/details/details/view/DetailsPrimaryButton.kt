package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.seasonal.seasonalButtonDecor
import su.afk.yummy.tv.feature.details.details.model.ButtonData
import su.afk.yummy.tv.feature.details.details.model.ButtonStyle

/** Крупная кнопка-пилюля главного действия: «Смотреть» залита акцентом, остальные — полупрозрачные. */
@Composable
internal fun DetailsPrimaryButton(
    button: ButtonData,
    focusedScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
) {
    // Неактивную кнопку приглушаем цветами, а не Modifier.alpha: слой с alpha < 1 рисуется в буфер
    // размером с кнопку и обрезает увеличение и рамку фокуса.
    val contentAlpha = if (button.enabled) 1f else DISABLED_ALPHA
    val colors = MaterialTheme.colorScheme
    val isFilled = button.style == ButtonStyle.Filled && button.overrideColor == null
    val override = button.overrideColor
    val background = dim(contentAlpha) {
        when {
            override != null -> override.copy(alpha = 0.2f)
            button.style == ButtonStyle.Filled -> colors.primary
            button.style == ButtonStyle.Outlined -> colors.primary
            else -> colors.onSurface.copy(alpha = 0.12f)
        }
    }
    val content = dim(contentAlpha) {
        when {
            override != null -> override
            button.style == ButtonStyle.Normal -> colors.onSurface
            else -> colors.onPrimary
        }
    }
    val outline = dim(contentAlpha) {
        when {
            override != null -> override.copy(alpha = 0.5f)
            button.style == ButtonStyle.Normal -> colors.onSurface.copy(alpha = 0.2f)
            else -> Color.Transparent
        }
    }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .tvFocusableClick(
                onClick = { if (button.enabled) onClick() },
                shape = PrimaryButtonShape,
                focusedScale = focusedScale,
                // На акцентной заливке рамка цвета акцента не видна.
                focusedBorderColor = if (isFilled) colors.onSurface else colors.primary,
            )
            .let { if (isFilled) it.seasonalButtonDecor() else it }
            .background(background, PrimaryButtonShape)
            .border(1.dp, outline, PrimaryButtonShape)
            .padding(horizontal = if (iconOnly) 0.dp else 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = button.icon,
            contentDescription = if (iconOnly) button.label else null,
            tint = content,
            modifier = Modifier.size(if (isFilled || iconOnly) 26.dp else 22.dp),
        )
        if (!iconOnly) {
            Text(
                text = button.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isFilled) FontWeight.Bold else FontWeight.SemiBold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val PrimaryButtonShape = RoundedCornerShape(50)

private inline fun dim(alpha: Float, color: () -> Color): Color =
    color().let { it.copy(alpha = it.alpha * alpha) }

internal const val DISABLED_ALPHA = 0.5f
