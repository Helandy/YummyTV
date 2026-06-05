package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.utils.label
import su.afk.yummy.tv.feature.library.utils.libraryTabsDisplayOrder

@Composable
internal fun LibraryTopTabs(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    contentFocusRequester: FocusRequester,
    tabFocusRequesters: Map<LibraryTab, FocusRequester>,
    mainMenuFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val tabs = remember { libraryTabsDisplayOrder() }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val effectiveFocusRequesters = tabs.map { tabFocusRequesters.getValue(it) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TvScreenPadding.Horizontal,
                top = TvScreenPadding.Vertical,
                end = TvScreenPadding.Horizontal,
            )
            .focusProperties {
                onEnter = {
                    effectiveFocusRequesters.getOrNull(selectedIndex)?.requestFocus()
                }
            }
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            LibraryTopTabItem(
                label = tab.label(),
                selected = selectedTab == tab,
                onActivated = {
                    onTabSelected(tab)
                    runCatching { contentFocusRequester.requestFocus() }
                },
                contentFocusRequester = contentFocusRequester,
                focusRequester = effectiveFocusRequesters[index],
                leftFocusRequester = effectiveFocusRequesters.getOrNull(index - 1)
                    ?: mainMenuFocusRequester.takeIf { index == 0 },
                rightFocusRequester = effectiveFocusRequesters.getOrNull(index + 1),
                onFocused = {
                    if (selectedTab != tab) onTabSelected(tab)
                },
            )
        }
    }
}
