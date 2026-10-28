package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.utils.label
import su.afk.yummy.tv.feature.library.utils.tabColor
import su.afk.yummy.tv.feature.library.utils.tabIcon

@Composable
internal fun LibraryTopTabs(
    tabs: List<LibraryTab>,
    selectedTab: LibraryTab,
    tabCounts: Map<LibraryTab, Int?>,
    contentCanFocus: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    contentFocusRequester: FocusRequester,
    tabFocusRequesters: Map<LibraryTab, FocusRequester>,
    mainMenuFocusRequester: FocusRequester?,
    /** Кнопка справа от вкладок (сортировка): куда уходит RIGHT с последней вкладки. */
    trailingFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val effectiveFocusRequesters = tabs.map { tabFocusRequesters.getValue(it) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties {
                onEnter = {
                    effectiveFocusRequesters.getOrNull(selectedIndex)?.requestFocus()
                }
            }
            .focusGroup(),
        // Ряд вкладок по центру на одной линии с аватаркой бокового меню, а не ниже неё.
        contentPadding = PaddingValues(
            start = TvScreenPadding.Horizontal,
            top = LibraryTopTabsTopPadding,
            end = TvScreenPadding.Horizontal,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(tabs, key = { _, tab -> tab.name }) { index, tab ->
            val selected = selectedTab == tab
            LibraryTopTabItem(
                label = tab.label(),
                icon = tab.tabIcon(),
                count = tabCounts[tab],
                color = tab.tabColor(),
                selected = selected,
                onActivated = {
                    onTabSelected(tab)
                    if (contentCanFocus) {
                        runCatching { contentFocusRequester.requestFocus() }
                    }
                },
                contentFocusRequester = contentFocusRequester,
                focusRequester = effectiveFocusRequesters[index],
                contentCanFocus = contentCanFocus,
                leftFocusRequester = effectiveFocusRequesters.getOrNull(index - 1)
                    ?: mainMenuFocusRequester.takeIf { index == 0 },
                rightFocusRequester = effectiveFocusRequesters.getOrNull(index + 1)
                    ?: trailingFocusRequester
                    ?: contentFocusRequester.takeIf { contentCanFocus },
                onFocused = {
                    if (selectedTab != tab) onTabSelected(tab)
                },
            )
        }
    }
}

private val LibraryTopTabsTopPadding = 14.dp
