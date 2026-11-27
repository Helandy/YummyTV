package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.onboarding.onboardingTarget
import su.afk.yummy.tv.core.designsystem.seasonal.SeasonalBarDecoration
import su.afk.yummy.tv.core.designsystem.seasonal.seasonalNavigationBarItemColors
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem
import su.afk.yummy.tv.feature.main.mobile.utils.mobileMainTabTargetId

/** Тот же размер области, что и у обычного бара: не меняет viewport и нижние inset-ы экранов. */
@Composable
internal fun <T> MobileFloatingNavigationBar(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    upFocusRequester: FocusRequester?,
    onDestinationSelected: (T) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
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
            // На узком экране приоритет у областей нажатия, а не у полной подписи.
            val extraWeight = if (menuItems.isEmpty()) 0f else
                (maxWidth / 48.dp - menuItems.size).coerceIn(0f, 1f)
            val colors = seasonalNavigationBarItemColors()
            Row(modifier = Modifier.fillMaxSize().selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                menuItems.forEach { item ->
                    val selected = item.destination == selectedDestination
                    val weight by animateFloatAsState(
                        targetValue = 1f + if (selected) extraWeight else 0f,
                        animationSpec = tween(220),
                        label = "navigation item width",
                    )
                    val indicator by animateColorAsState(
                        targetValue = if (selected) colors.selectedIndicatorColor else Color.Transparent,
                        animationSpec = tween(220),
                        label = "navigation indicator",
                    )
                    Box(
                        modifier = Modifier
                            .weight(weight)
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
                                        style = MaterialTheme.typography.labelLarge,
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
