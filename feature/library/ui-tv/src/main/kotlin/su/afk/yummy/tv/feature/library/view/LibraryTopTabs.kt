package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
    selectedTabFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val tabs = remember { libraryTabsDisplayOrder() }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }
    val tabFocusRequesters = remember(tabs.size) { List(tabs.size) { FocusRequester() } }
    val effectiveFocusRequesters = tabs.mapIndexed { index, _ ->
        if (index == selectedIndex) selectedTabFocusRequester else tabFocusRequesters[index]
    }

    LaunchedEffect(selectedIndex) {
        focusedIndex = selectedIndex
    }

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
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (focusedIndex > 0) {
                            focusedIndex -= 1
                            effectiveFocusRequesters[focusedIndex].requestFocus()
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (focusedIndex < effectiveFocusRequesters.lastIndex) {
                            focusedIndex += 1
                            effectiveFocusRequesters[focusedIndex].requestFocus()
                        }
                        true
                    }

                    else -> false
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
                onSelected = { onTabSelected(tab) },
                contentFocusRequester = contentFocusRequester,
                focusRequester = effectiveFocusRequesters[index],
                leftFocusRequester = effectiveFocusRequesters.getOrNull(index - 1),
                rightFocusRequester = effectiveFocusRequesters.getOrNull(index + 1),
                onFocused = { focusedIndex = index },
            )
        }
    }
}
