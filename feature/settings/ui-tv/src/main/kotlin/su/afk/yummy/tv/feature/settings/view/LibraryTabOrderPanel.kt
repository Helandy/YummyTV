package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft

@Composable
internal fun LibraryTabOrderPanel(
    order: List<LibraryTabKind>,
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester?,
    onMoveUp: (LibraryTabKind) -> Unit,
    onMoveDown: (LibraryTabKind) -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailsButtonOrderResetRow(
            onReset = onReset,
            label = stringResource(R.string.settings_library_tabs_reset),
            hint = stringResource(R.string.settings_library_tabs_reset_hint),
            modifier = Modifier
                .then(
                    if (contentFocusRequester != null) {
                        Modifier.focusRequester(contentFocusRequester)
                    } else {
                        Modifier
                    },
                )
                .restoreCategoryFocusOnLeft(upFocusRequester),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
        order.forEachIndexed { index, tab ->
            key(tab) {
                DetailsButtonOrderRow(
                    label = tab.label(),
                    position = index + 1,
                    canMoveUp = !tab.isPinnedFirst && index > 0 && !order[index - 1].isPinnedFirst,
                    canMoveDown = !tab.isPinnedFirst && index < order.lastIndex,
                    onMoveUp = { onMoveUp(tab) },
                    onMoveDown = { onMoveDown(tab) },
                )
            }
            if (index < order.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}
