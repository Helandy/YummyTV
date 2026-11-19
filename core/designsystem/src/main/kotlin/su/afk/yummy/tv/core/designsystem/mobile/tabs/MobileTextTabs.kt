package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val INDICATOR_HEIGHT = 3.dp
private val INDICATOR_TOP_RADIUS = 3.dp
private val DIVIDER_HEIGHT = 1.dp

/**
 * Текстовые вкладки с подчёркиванием выбранной. [scrollable] — для длинных списков
 * (категории), иначе вкладки делят ширину поровну (сортировки).
 *
 * Индикатор плавно перетекает между соседними вкладками с прогрессом [transition].
 * Свой layout и отрисовка вместо `material3.TabRow`: его subcompose-индикатор падает в Compose
 * («LayoutNode not found in RectList») при уходе экрана с вкладками.
 */
@Composable
fun MobileTextTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    transition: MobileTabTransition? = null,
) {
    if (titles.isEmpty()) return
    if (scrollable) {
        MobileScrollableTabs(
            tabCount = titles.size,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            modifier = modifier,
            transition = transition,
            edgePadding = 12.dp,
            showDivider = false,
        ) { index, _ ->
            MobileTextTabLabel(
                title = titles[index],
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    } else {
        val resolvedTransition = transition ?: rememberAnimatedTabTransition(selectedIndex, titles.size)
        val tabBounds = remember(titles.size) {
            mutableStateListOf<Rect?>().apply { repeat(titles.size) { add(null) } }
        }
        val indicatorColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

        Row(
            modifier = modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawFluidUnderlineIndicator(
                        bounds = tabBounds,
                        transition = resolvedTransition,
                        color = indicatorColor,
                        indicatorHeightPx = INDICATOR_HEIGHT.toPx(),
                        topCornerRadiusPx = INDICATOR_TOP_RADIUS.toPx(),
                    )
                },
        ) {
            titles.forEachIndexed { index, title ->
                val selectionFraction = resolvedTransition.selectionFraction(index)
                val color = lerp(unselectedColor, indicatorColor, selectionFraction)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .onPlaced { coordinates ->
                            val rect = coordinates.boundsInParent()
                            if (tabBounds[index] != rect) {
                                tabBounds[index] = rect
                            }
                        }
                        .selectable(
                            selected = index == resolvedTransition.activeIndex,
                            role = Role.Tab,
                            onClick = { onSelected(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompositionLocalProvider(LocalContentColor provides color) {
                        MobileTextTabLabel(
                            title = title,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                    Spacer(Modifier.height(INDICATOR_HEIGHT))
                }
            }
        }
    }
}

/**
 * Прокручиваемый ряд вкладок с произвольным содержимым (например, подпись + счётчик) и
 * плавно перетекающим нижним индикатором без `SubcomposeLayout`.
 */
@Composable
fun MobileScrollableTabs(
    tabCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    transition: MobileTabTransition? = null,
    edgePadding: Dp = 12.dp,
    showDivider: Boolean = false,
    tabContent: @Composable (index: Int, selectionFraction: Float) -> Unit,
) {
    if (tabCount <= 0) return
    val resolvedTransition = transition ?: rememberAnimatedTabTransition(selectedIndex, tabCount)
    val listState = rememberLazyListState()
    val indicatorColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val activeIndex = resolvedTransition.activeIndex
    LaunchedEffect(activeIndex, tabCount) {
        if (activeIndex in 0 until tabCount) {
            listState.animateScrollTabIntoCenter(activeIndex)
        }
    }

    LazyRow(
        modifier = modifier.drawWithContent {
            drawContent()
            if (showDivider) {
                val strokePx = DIVIDER_HEIGHT.toPx()
                val y = size.height - strokePx / 2f
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokePx,
                )
            }
            drawLazyRowUnderlineIndicator(
                listState = listState,
                transition = resolvedTransition,
                color = indicatorColor,
                indicatorHeightPx = INDICATOR_HEIGHT.toPx(),
                topCornerRadiusPx = INDICATOR_TOP_RADIUS.toPx(),
            )
        },
        state = listState,
        contentPadding = PaddingValues(horizontal = edgePadding),
    ) {
        items(count = tabCount, key = { it }) { index ->
            val selectionFraction = resolvedTransition.selectionFraction(index)
            val color = lerp(unselectedColor, indicatorColor, selectionFraction)
            Column(
                modifier = Modifier.selectable(
                    selected = index == activeIndex,
                    role = Role.Tab,
                    onClick = { onSelected(index) },
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompositionLocalProvider(LocalContentColor provides color) {
                    tabContent(index, selectionFraction)
                }
                Spacer(Modifier.height(INDICATOR_HEIGHT))
            }
        }
    }
}

@Composable
private fun MobileTextTabLabel(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        color = LocalContentColor.current,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
