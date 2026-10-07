package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

@Composable
internal fun SettingsMobileLibraryTabOrder(
    order: List<LibraryTabKind>,
    onMove: (LibraryTabKind, DetailsButtonMoveDirection) -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(stringResource(R.string.settings_library_tabs_reset))
        }
        order.forEachIndexed { index, tab ->
            DetailsOrderRow(
                label = tab.label(),
                position = index + 1,
                canMoveUp = !tab.isPinnedFirst && index > 0 && !order[index - 1].isPinnedFirst,
                canMoveDown = !tab.isPinnedFirst && index < order.lastIndex,
                onMoveUp = { onMove(tab, DetailsButtonMoveDirection.UP) },
                onMoveDown = { onMove(tab, DetailsButtonMoveDirection.DOWN) },
            )
        }
    }
}
