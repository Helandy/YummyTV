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
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var focusedIndex by remember { mutableIntStateOf(LibraryTab.entries.indexOf(selectedTab).coerceAtLeast(0)) }
    val itemFocusRequesters = remember { List(LibraryTab.entries.size) { FocusRequester() } }
    val panelWidth by animateDpAsState(
        targetValue = if (expanded) 148.dp else 52.dp,
        animationSpec = tween(durationMillis = 200),
        label = "library_panel_width",
    )

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
                            itemFocusRequesters[focusedIndex].requestFocus()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> {
                        if (focusedIndex < itemFocusRequesters.lastIndex) {
                            focusedIndex += 1
                            itemFocusRequesters[focusedIndex].requestFocus()
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
                focusRequester = itemFocusRequesters[index],
                upFocusRequester = itemFocusRequesters.getOrNull(index - 1),
                downFocusRequester = itemFocusRequesters.getOrNull(index + 1) ?: FocusRequester.Cancel,
                onFocused = { focusedIndex = index },
            )
        }
    }
}

@Composable
private fun LibraryTab.label(): String = stringResource(
    when (this) {
        LibraryTab.LIST -> R.string.library_tab_list
        LibraryTab.CONTINUE_WATCHING -> R.string.library_tab_continue_watching
    },
)

@Composable
private fun LibraryTab.shortLabel(): String = stringResource(
    when (this) {
        LibraryTab.LIST -> R.string.library_tab_list_short
        LibraryTab.CONTINUE_WATCHING -> R.string.library_tab_continue_watching_short
    },
)
