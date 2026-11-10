package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.color
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.core.model.settings.HomeBlockConfig
import su.afk.yummy.tv.core.model.settings.HomeBlockKind
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.presentation.R

@Composable
internal fun SettingsMobileHomeBlocks(
    blocks: List<HomeBlockConfig>,
    onToggle: (HomeBlockKind) -> Unit,
    onMove: (HomeBlockKind, DetailsButtonMoveDirection) -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(stringResource(R.string.settings_home_blocks_reset))
        }
        blocks.forEachIndexed { index, block ->
            key(block.kind) {
                DetailsOrderRow(
                    label = block.kind.label(),
                    position = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < blocks.lastIndex,
                    onMoveUp = { onMove(block.kind, DetailsButtonMoveDirection.UP) },
                    onMoveDown = { onMove(block.kind, DetailsButtonMoveDirection.DOWN) },
                    icon = block.kind.icon(),
                    iconColor = block.kind.color(),
                    trailing = {
                        Switch(checked = block.enabled, onCheckedChange = { onToggle(block.kind) })
                    },
                )
            }
        }
    }
}
