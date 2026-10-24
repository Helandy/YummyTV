package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Пункт рейки без подписи. Стандартный `NavigationRailItem` без label заливает индикатор кругом 56dp вокруг иконки,
 * на широком экране фолда это слишком много: здесь индикатор — таблетка 56×32, как в нижнем баре.
 */
@Composable
internal fun MobileMainNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    colors: NavigationRailItemColors,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = IndicatorWidth, height = ItemHeight)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = IndicatorWidth, height = IndicatorHeight)
                .clip(CircleShape)
                .background(if (selected) colors.selectedIndicatorColor else Color.Transparent)
                .indication(interactionSource, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) colors.selectedIconColor else colors.unselectedIconColor,
                content = icon,
            )
        }
    }
}

private val ItemHeight = 56.dp
private val IndicatorWidth = 56.dp
private val IndicatorHeight = 32.dp
