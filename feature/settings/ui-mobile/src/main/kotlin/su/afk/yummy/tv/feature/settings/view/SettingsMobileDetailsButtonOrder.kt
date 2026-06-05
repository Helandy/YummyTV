package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.feature.settings.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.utils.toDetailsButtonOrderItems

@Composable
internal fun SettingsMobileDetailsButtonOrder(
    order: List<DetailsButtonAction>,
    onMove: (DetailsButtonAction, DetailsButtonMoveDirection) -> Unit,
    onReset: () -> Unit,
) {
    val items = order.toDetailsButtonOrderItems()
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(stringResource(R.string.settings_details_buttons_reset))
        }
        items.forEachIndexed { index, item ->
            DetailsOrderRow(
                label = item.label,
                position = index + 1,
                canMoveUp = index > 0,
                canMoveDown = index < items.lastIndex,
                onMoveUp = { onMove(item.action, DetailsButtonMoveDirection.UP) },
                onMoveDown = { onMove(item.action, DetailsButtonMoveDirection.DOWN) },
            )
        }
    }
}

@Composable
private fun DetailsOrderRow(
    label: String,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.settings_details_button_position, position),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.settings_details_button_move_up),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.settings_details_button_move_down),
            )
        }
    }
}
