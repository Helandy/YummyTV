package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Прокручиваемый ряд карточек-вкладок со скруглённой плашкой-индикатором (например, даты расписания).
 *
 * Прогресс [transition] читается только в фазе отрисовки и внутри провайдера `selectionFraction`,
 * передаваемого в [itemContent], поэтому свайп не вызывает покадровой рекомпозиции.
 */
@Composable
fun MobileScrollablePillTabs(
    tabCount: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    transition: (() -> MobileTabTransition)? = null,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorCornerRadius: Dp = 10.dp,
    horizontalSpacing: Dp = 10.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 4.dp,
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable (index: Int, selected: Boolean, selectionFraction: () -> Float) -> Unit,
) {
    if (tabCount <= 0) return
    val transitionProvider = transition ?: rememberAnimatedTabTransition(selectedIndex, tabCount)
    val listState = rememberLazyListState()

    val activeIndex by remember(transitionProvider) {
        derivedStateOf { transitionProvider().activeIndex }
    }
    val scrollTargetIndex by remember(transitionProvider) {
        derivedStateOf { transitionProvider().lazyRowScrollTarget() }
    }

    LaunchedEffect(scrollTargetIndex, tabCount) {
        if (scrollTargetIndex in 0 until tabCount) {
            listState.animateScrollTabIntoCenter(scrollTargetIndex)
        }
    }

    LazyRow(
        modifier = modifier.drawBehind {
            drawLazyRowPillIndicator(
                listState = listState,
                transition = transitionProvider(),
                color = indicatorColor,
                cornerRadius = CornerRadius(indicatorCornerRadius.toPx()),
                verticalInsetPx = verticalPadding.toPx(),
            )
        },
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        items(
            count = tabCount,
            key = key ?: { it },
        ) { index ->
            val selectionFraction = remember(transitionProvider, index) {
                { transitionProvider().selectionFraction(index) }
            }
            itemContent(index, index == activeIndex, selectionFraction)
        }
    }
}
