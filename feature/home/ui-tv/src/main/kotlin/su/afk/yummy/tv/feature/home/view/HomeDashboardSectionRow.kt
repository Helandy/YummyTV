package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSection

/**
 * Один ряд секции главной. Вынесен из `HomeDashboard`, потому что секции рисуются в двух местах:
 * закреплённый ряд новых серий идёт до «Аниме сезона», остальные — после него.
 */
@Composable
internal fun HomeDashboardSectionRow(
    section: HomeFeedSection,
    rowKey: String,
    rowIsFocused: Boolean,
    rowFocusRequester: FocusRequester,
    restoreItemKey: String?,
    showYear: Boolean,
    bottomPadding: Dp,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    /** [justEntered] — фокус пришёл в ряд извне, а не перешёл между карточками внутри него. */
    onRowFocused: (justEntered: Boolean) -> Unit,
    registerFocusHandler: ((suspend () -> Boolean)?) -> Unit,
    onItemSelected: (sectionId: String, item: HomeFeedItem) -> Unit,
    onItemLongClick: ((HomeFeedItem) -> Unit)?,
    onFocusedItemKeyChanged: (String) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    var rowHadFocus by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.onFocusChanged { state ->
            val hadFocus = rowHadFocus
            rowHadFocus = state.hasFocus
            if (state.hasFocus) onRowFocused(!hadFocus)
        },
    ) {
        HomeSection(
            title = section.title,
            items = section.items,
            showYear = showYear,
            onItemSelected = onItemSelected,
            onItemLongClick = onItemLongClick,
            rowFocusRequester = rowFocusRequester,
            registerFocusHandler = registerFocusHandler,
            rowIsFocused = rowIsFocused,
            rowKey = rowKey,
            restoreItemKey = restoreItemKey,
            onFocusedItemKeyChanged = onFocusedItemKeyChanged,
            upFocusRequester = upFocusRequester,
            downFocusRequester = downFocusRequester,
            bottomPadding = bottomPadding,
            focusedCardScale = 1f,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )
    }
}
