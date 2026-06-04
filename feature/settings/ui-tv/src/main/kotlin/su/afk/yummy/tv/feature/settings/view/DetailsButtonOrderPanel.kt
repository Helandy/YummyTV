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
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.feature.settings.utils.restoreTabFocusOnUp
import su.afk.yummy.tv.feature.settings.utils.toDetailsButtonOrderItems

@Composable
internal fun DetailsButtonOrderPanel(
    order: List<DetailsButtonAction>,
    upFocusRequester: FocusRequester,
    onMoveUp: (DetailsButtonAction) -> Unit,
    onMoveDown: (DetailsButtonAction) -> Unit,
    onReset: () -> Unit,
) {
    val items = order.toDetailsButtonOrderItems()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailsButtonOrderResetRow(
            onReset = onReset,
            modifier = Modifier.restoreTabFocusOnUp(upFocusRequester),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
        items.forEachIndexed { index, item ->
            key(item.key) {
                DetailsButtonOrderRow(
                    label = item.label,
                    position = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < items.lastIndex,
                    onMoveUp = { onMoveUp(item.action) },
                    onMoveDown = { onMoveDown(item.action) },
                )
            }
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}
