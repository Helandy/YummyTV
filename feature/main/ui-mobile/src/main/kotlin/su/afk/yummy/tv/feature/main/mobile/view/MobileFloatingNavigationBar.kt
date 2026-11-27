package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.onboarding.onboardingTarget
import su.afk.yummy.tv.core.designsystem.seasonal.SeasonalBarDecoration
import su.afk.yummy.tv.core.designsystem.seasonal.seasonalNavigationBarItemColors
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem
import su.afk.yummy.tv.feature.main.mobile.utils.mobileMainTabTargetId

/** Панель без фона вокруг капсулы: корневой каркас размещает её поверх контента. */
@Composable
internal fun <T> MobileFloatingNavigationBar(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    upFocusRequester: FocusRequester?,
    onDestinationSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(MobileBottomBarDefaults.BarHeight)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 4.dp),
        ) {
            val textStyle = MaterialTheme.typography.labelLarge
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            // Одна ширина для любого выбранного пункта: соседние иконки едут плавно, без скачка
            // при переходе от короткого названия к длинному и обратно.
            val labelWidths = menuItems.map { item ->
                with(density) {
                    textMeasurer.measure(item.label, style = textStyle, maxLines = 1, softWrap = false).size.width.toDp()
                }
            }
            val longestLabel = labelWidths.maxOrNull() ?: 0.dp
            val availableWidth = maxWidth
            val widths = floatingNavigationWidths(availableWidth, menuItems.size, longestLabel)
            val hasSelection = menuItems.any { it.destination == selectedDestination }
            val colors = seasonalNavigationBarItemColors()
            Row(modifier = Modifier.fillMaxSize().selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                menuItems.forEachIndexed { index, item ->
                    val selected = item.destination == selectedDestination
                    val width by animateDpAsState(
                        targetValue = when {
                            !hasSelection -> availableWidth / menuItems.size
                            selected -> widths.selected
                            else -> widths.unselected
                        },
                        animationSpec = tween(220),
                        label = "navigation item width",
                    )
                    val availableLabel = (widths.selected - FloatingItemContentWidth).coerceAtLeast(0.dp)
                    val labelWidth = labelWidths[index]
                    val textScale = if (labelWidth > 0.dp) {
                        (availableLabel / labelWidth).coerceIn(0.72f, 1f)
                    } else {
                        1f
                    }
                    val indicator by animateColorAsState(
                        targetValue = if (selected) colors.selectedIndicatorColor else Color.Transparent,
                        animationSpec = tween(220),
                        label = "navigation indicator",
                    )
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(48.dp)
                            .testTag("main_tab")
                            .onboardingTarget(mobileMainTabTargetId(item.destination))
                            .clickablePointer()
                            .focusProperties { upFocusRequester?.takeIf { selected }?.let { up = it } }
                            .clip(CircleShape)
                            .background(indicator)
                            .selectable(selected = selected, onClick = { onDestinationSelected(item.destination) }, role = Role.Tab),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (selected) colors.selectedIconColor else colors.unselectedIconColor,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MobileMainNavigationIcon(item, selected)
                                AnimatedVisibility(
                                    visible = selected,
                                    modifier = Modifier.weight(1f, fill = false),
                                    enter = expandHorizontally(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                                    exit = shrinkHorizontally(animationSpec = tween(220)) + fadeOut(animationSpec = tween(220)),
                                ) {
                                    // У иконки уже есть contentDescription; не озвучивать название дважды.
                                    Text(
                                        text = item.label,
                                        modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics {}.sizeIn(minWidth = 0.dp),
                                        style = textStyle.copy(fontSize = textStyle.fontSize * textScale),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SeasonalBarDecoration(Modifier.align(Alignment.TopCenter))
        }
    }
}

/** Ширина выбранного пункта зависит от самой длинной подписи, а не от текущей вкладки. */
internal data class FloatingNavigationWidths(val selected: Dp, val unselected: Dp)

internal fun floatingNavigationWidths(available: Dp, count: Int, longestLabel: Dp): FloatingNavigationWidths {
    if (count <= 0) return FloatingNavigationWidths(0.dp, 0.dp)
    if (count == 1) return FloatingNavigationWidths(available, 0.dp)

    // Обычно у каждого пункта 48 dp; на узком экране допускаем 44 dp, чтобы название не обрезалось.
    val minUnselected = minOf(48.dp, (available - longestLabel - FloatingItemContentWidth) / (count - 1))
        .coerceAtLeast(minOf(44.dp, available / count))
    val selected = (longestLabel + FloatingItemContentWidth)
        .coerceAtLeast(48.dp.coerceAtMost(available / count))
        .coerceAtMost(available - minUnselected * (count - 1))
    return FloatingNavigationWidths(selected, (available - selected) / (count - 1))
}

// Иконка/аватар до 26 dp, отступы по 8 dp и 6 dp между иконкой и подписью.
private val FloatingItemContentWidth = 48.dp
