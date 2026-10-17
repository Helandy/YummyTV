package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Пункт бокового меню настроек ТВ-плеера: название слева, текущее значение справа. С [showChevron]
 * OK открывает подменю со списком вариантов, без него — действует сразу (переключатель).
 */
@Composable
internal fun TvPlayerSettingsMenuRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
    /** Цвет значения вне фокуса — например, сам цвет субтитров; в фокусе текст обычный, для контраста. */
    valueColor: Color? = null,
    /** Не null — вместо текста значения рисуется переключатель вкл/выкл. */
    toggleChecked: Boolean? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (focused) colors.primary else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvSettingsMenuRowBackground",
    )
    // Голый Text на ТВ рисуется чёрным, поэтому цвет задаём явно.
    val contentColor = if (focused) colors.onPrimary else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (toggleChecked != null) {
            TvPlayerSettingsToggle(checked = toggleChecked, focused = focused, modifier = Modifier.padding(end = 6.dp))
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (focused) contentColor else valueColor ?: contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
