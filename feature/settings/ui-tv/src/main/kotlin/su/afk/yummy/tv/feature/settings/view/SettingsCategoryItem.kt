package su.afk.yummy.tv.feature.settings.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Пункт левого списка категорий настроек. Активирование (вправо/Enter) уводит фокус в панель справа. */
@Composable
internal fun SettingsCategoryItem(
    label: String,
    selected: Boolean,
    contentFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    onSelected: () -> Unit,
    onActivated: () -> Unit = onSelected,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = when {
            focused -> colors.primary
            selected -> colors.primary.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "settingsCategoryItemBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            focused -> colors.onPrimary
            selected -> colors.primary
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(150),
        label = "settingsCategoryItemContent",
    )
    val accentColor = when {
        focused -> colors.onPrimary
        selected -> colors.primary
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties {
                upFocusRequester?.let { up = it }
                downFocusRequester?.let { down = it }
                leftFocusRequester?.let { left = it }
                right = contentFocusRequester
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        leftFocusRequester?.let {
                            runCatching { it.requestFocus() }
                            true
                        } ?: false
                    }

                    Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onActivated()
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged { if (it.isFocused) onSelected() }
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null) { onActivated() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
