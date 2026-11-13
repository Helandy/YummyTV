package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.details.model.ButtonData

/** Круглая кнопка второстепенного действия; подпись показывает ряд под иконками, счётчик — бейдж в углу. */
@Composable
internal fun DetailsIconButton(
    button: ButtonData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // Без Modifier.alpha: слой с alpha < 1 обрезал бы увеличение и рамку фокуса — приглушаем цветами.
    val contentAlpha = if (button.enabled) 1f else DISABLED_ALPHA
    // Внешний modifier (фокус-реквестер, направления) — на сам фокусируемый круг, а не на обёртку с бейджем.
    Box {
        Box(
            modifier = modifier
                .size(IconButtonSize)
                .tvFocusableClick(
                    onClick = { if (button.enabled) onClick() },
                    shape = CircleShape,
                    focusedScale = IconFocusedScale,
                )
                .background(colors.onSurface.copy(alpha = 0.12f * contentAlpha), CircleShape)
                .border(1.dp, colors.onSurface.copy(alpha = 0.18f * contentAlpha), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = button.icon,
                contentDescription = button.label,
                tint = colors.onSurface.copy(alpha = contentAlpha),
                modifier = Modifier.size(22.dp),
            )
        }
        button.badge?.let { badge ->
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-4).dp)
                    .background(colors.primary, RoundedCornerShape(50))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

private val IconButtonSize = 48.dp
private const val IconFocusedScale = 1.12f
