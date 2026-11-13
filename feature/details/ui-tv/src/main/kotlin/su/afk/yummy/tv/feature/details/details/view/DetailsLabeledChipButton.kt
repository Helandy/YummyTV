package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.details.model.ButtonData

/** Компактная пилюля с иконкой и текстом во втором ряду кнопок («Серии», «Подписки»). */
@Composable
internal fun DetailsLabeledChipButton(
    button: ButtonData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // Как и у остальных кнопок — приглушаем цветами, а не Modifier.alpha, чтобы не резать рамку фокуса.
    val contentAlpha = if (button.enabled) 1f else DISABLED_ALPHA
    val content = colors.onSurface.copy(alpha = contentAlpha)
    Row(
        modifier = modifier
            .height(LabeledChipHeight)
            .tvFocusableClick(
                onClick = { if (button.enabled) onClick() },
                shape = LabeledChipShape,
                focusedScale = LabeledChipFocusedScale,
            )
            .background(colors.onSurface.copy(alpha = 0.12f * contentAlpha), LabeledChipShape)
            .border(1.dp, colors.onSurface.copy(alpha = 0.18f * contentAlpha), LabeledChipShape)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = button.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = button.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val LabeledChipShape = RoundedCornerShape(50)
private val LabeledChipHeight = 48.dp
private const val LabeledChipFocusedScale = 1.06f
