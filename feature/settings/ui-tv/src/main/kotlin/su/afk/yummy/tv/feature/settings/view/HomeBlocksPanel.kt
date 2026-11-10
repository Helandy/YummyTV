package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.color
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.core.model.settings.HomeBlockConfig
import su.afk.yummy.tv.core.model.settings.HomeBlockKind
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft

@Composable
internal fun HomeBlocksPanel(
    blocks: List<HomeBlockConfig>,
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester?,
    onToggle: (HomeBlockKind) -> Unit,
    onMoveUp: (HomeBlockKind) -> Unit,
    onMoveDown: (HomeBlockKind) -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailsButtonOrderResetRow(
            onReset = onReset,
            label = stringResource(R.string.settings_home_blocks_reset),
            hint = stringResource(R.string.settings_home_blocks_reset_hint),
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
        blocks.forEachIndexed { index, block ->
            key(block.kind) {
                DetailsButtonOrderRow(
                    label = block.kind.label(),
                    position = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < blocks.lastIndex,
                    onMoveUp = { onMoveUp(block.kind) },
                    onMoveDown = { onMoveDown(block.kind) },
                    icon = block.kind.icon(),
                    iconColor = block.kind.color(),
                    trailing = {
                        FocusableIconButton(
                            icon = if (block.enabled) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = stringResource(
                                if (block.enabled) {
                                    R.string.settings_home_block_shown
                                } else {
                                    R.string.settings_home_block_hidden
                                },
                            ),
                            enabled = true,
                            onClick = { onToggle(block.kind) },
                        )
                    },
                )
            }
            if (index < blocks.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}
