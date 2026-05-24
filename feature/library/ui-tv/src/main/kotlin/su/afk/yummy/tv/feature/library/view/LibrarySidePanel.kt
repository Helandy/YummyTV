package su.afk.yummy.tv.feature.library.view

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.R

@Composable
internal fun LibrarySidePanel(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    contentFocusRequester: FocusRequester,
    selectedTabFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = LibraryTab.entries.indexOf(selectedTab).coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }
    val itemFocusRequesters = remember { List(LibraryTab.entries.size) { FocusRequester() } }
    val effectiveFocusRequesters = LibraryTab.entries.mapIndexed { index, _ ->
        if (index == selectedIndex) selectedTabFocusRequester else itemFocusRequesters[index]
    }
    val panelWidth by animateDpAsState(
        targetValue = if (expanded) 148.dp else 52.dp,
        animationSpec = tween(durationMillis = 200),
        label = "library_panel_width",
    )

    LaunchedEffect(selectedIndex) {
        focusedIndex = selectedIndex
    }

    Column(
        modifier = modifier
            .width(panelWidth)
            .fillMaxHeight()
            .onFocusChanged { expanded = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        if (focusedIndex > 0) {
                            focusedIndex -= 1
                            effectiveFocusRequesters[focusedIndex].requestFocus()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> {
                        if (focusedIndex < effectiveFocusRequesters.lastIndex) {
                            focusedIndex += 1
                            effectiveFocusRequesters[focusedIndex].requestFocus()
                        }
                        true
                    }
                    else -> false
                }
            }
            .focusGroup()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
            )
            .padding(vertical = 24.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LibraryTab.entries.forEachIndexed { index, tab ->
            LibrarySidePanelItem(
                label = tab.label(),
                shortLabel = tab.shortLabel(),
                selected = selectedTab == tab,
                expanded = expanded,
                onSelected = { onTabSelected(tab) },
                contentFocusRequester = contentFocusRequester,
                focusRequester = effectiveFocusRequesters[index],
                upFocusRequester = effectiveFocusRequesters.getOrNull(index - 1),
                downFocusRequester = effectiveFocusRequesters.getOrNull(index + 1) ?: FocusRequester.Cancel,
                onFocused = { focusedIndex = index },
            )
        }
    }
}

@Composable
private fun LibraryTab.label(): String = stringResource(
    when (this) {
        LibraryTab.CONTINUE_WATCHING -> R.string.library_tab_continue_watching
        LibraryTab.WATCHING -> R.string.library_tab_watching
        LibraryTab.PLANNED -> R.string.library_tab_planned
        LibraryTab.COMPLETED -> R.string.library_tab_completed
        LibraryTab.POSTPONED -> R.string.library_tab_postponed
        LibraryTab.DROPPED -> R.string.library_tab_dropped
    },
)

@Composable
private fun LibraryTab.shortLabel(): String = stringResource(
    when (this) {
        LibraryTab.CONTINUE_WATCHING -> R.string.library_tab_continue_watching_short
        LibraryTab.WATCHING -> R.string.library_tab_watching_short
        LibraryTab.PLANNED -> R.string.library_tab_planned_short
        LibraryTab.COMPLETED -> R.string.library_tab_completed_short
        LibraryTab.POSTPONED -> R.string.library_tab_postponed_short
        LibraryTab.DROPPED -> R.string.library_tab_dropped_short
    },
)
