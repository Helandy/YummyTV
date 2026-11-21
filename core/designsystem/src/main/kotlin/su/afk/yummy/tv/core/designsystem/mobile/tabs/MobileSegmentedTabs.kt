package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val CONTAINER_CORNER_RADIUS = 8.dp
private val INDICATOR_CORNER_RADIUS = 6.dp
private val CONTAINER_PADDING = 4.dp
private val ITEM_SPACING = 4.dp
private const val CONTAINER_ALPHA = 0.34f

/**
 * Сегментные вкладки-плашки в общей подложке (профиль, настройки дорожек плеера).
 *
 * Активная плашка и цвет содержимого читают [transition] только в фазе отрисовки
 * (через `drawBehind` и [ColorProducer]), не вызывая покадровой рекомпозиции при свайпе.
 */
@Composable
fun MobileSegmentedTabs(
    tabCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    transition: (() -> MobileTabTransition)? = null,
    equalWeight: Boolean = true,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tabContent: @Composable (index: Int, selectionFraction: () -> Float, contentColor: ColorProducer) -> Unit,
) {
    if (tabCount <= 0) return
    val transitionProvider = transition ?: rememberAnimatedTabTransition(selectedIndex, tabCount)
    val activeIndex by remember(transitionProvider) {
        derivedStateOf { transitionProvider().activeIndex }
    }
    val tabBounds = remember(tabCount) {
        mutableStateListOf<Rect?>().apply { repeat(tabCount) { add(null) } }
    }
    val indicatorShape = RoundedCornerShape(INDICATOR_CORNER_RADIUS)

    Surface(
        modifier = if (equalWeight) modifier.fillMaxWidth() else modifier,
        shape = RoundedCornerShape(CONTAINER_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = CONTAINER_ALPHA),
    ) {
        Row(
            modifier = (if (equalWeight) Modifier.fillMaxWidth() else Modifier)
                .height(IntrinsicSize.Min)
                .padding(CONTAINER_PADDING)
                .drawBehind {
                    drawFluidPillIndicator(
                        boundsAt = tabBounds::getOrNull,
                        transition = transitionProvider(),
                        cornerRadius = CornerRadius(INDICATOR_CORNER_RADIUS.toPx()),
                        colorForIndex = { indicatorColor },
                        areAdjacent = ::areAdjacentRowTabs,
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(tabCount) { index ->
                val selectionFraction = remember(transitionProvider, index) {
                    { transitionProvider().selectionFraction(index) }
                }
                val contentColor = remember(selectionFraction, unselectedContentColor, selectedContentColor) {
                    ColorProducer { lerp(unselectedContentColor, selectedContentColor, selectionFraction()) }
                }
                Box(
                    modifier = (if (equalWeight) Modifier.weight(1f) else Modifier)
                        .fillMaxHeight()
                        .onPlaced { coordinates ->
                            val rect = coordinates.boundsInParent()
                            if (tabBounds[index] != rect) {
                                tabBounds[index] = rect
                            }
                        }
                        .clip(indicatorShape)
                        .selectable(
                            selected = index == activeIndex,
                            role = Role.Tab,
                            onClick = { onSelected(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    tabContent(index, selectionFraction, contentColor)
                }
            }
        }
    }
}
