package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.color
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.core.model.settings.DetailsSectionConfig
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind
import su.afk.yummy.tv.feature.settings.common.utils.label
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft

@Composable
internal fun DetailsSectionsPanel(
    sections: List<DetailsSectionConfig>,
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester?,
    onToggle: (DetailsSectionKind) -> Unit,
    onMoveUp: (DetailsSectionKind) -> Unit,
    onMoveDown: (DetailsSectionKind) -> Unit,
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
        Text(
            text = stringResource(R.string.settings_details_sections_tv_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
        sections.forEachIndexed { index, section ->
            key(section.kind) {
                DetailsButtonOrderRow(
                    label = section.kind.label(),
                    position = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < sections.lastIndex,
                    onMoveUp = { onMoveUp(section.kind) },
                    onMoveDown = { onMoveDown(section.kind) },
                    icon = section.kind.icon(),
                    iconColor = section.kind.color(),
                    trailing = {
                        FocusableIconButton(
                            icon = if (section.enabled) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = stringResource(
                                if (section.enabled) {
                                    R.string.settings_home_block_shown
                                } else {
                                    R.string.settings_home_block_hidden
                                },
                            ),
                            enabled = true,
                            onClick = { onToggle(section.kind) },
                        )
                    },
                )
            }
            if (index < sections.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}
