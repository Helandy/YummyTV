package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import su.afk.yummy.tv.core.designsystem.utils.dubbingLogoRes
import su.afk.yummy.tv.feature.player.common.utils.formatCompactCount
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSourceOption
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Диалог «в этом плеере серии нет»: озвучки, где она есть, строками как в панели выбора озвучки.
 * Фокус по умолчанию получает первая строка через [firstFocusRequester] (его запрашивает
 * TvPlayerFocusEffects).
 */
@Composable
internal fun TvPlayerNextEpisodeSourcesPrompt(
    title: String,
    subtitle: String?,
    options: ImmutableList<PlayerNextEpisodeSourceOption>,
    firstFocusRequester: FocusRequester,
    onSelect: (PlayerNextEpisodeSourceOption) -> Unit,
    onCancel: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = TV_PLAYER_PROMPT_SIDE_MARGIN)
            .widthIn(max = TV_PLAYER_PROMPT_MAX_WIDTH)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
        Column(
            modifier = Modifier
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            options.forEachIndexed { index, option ->
                PlayerSelectionItem(
                    label = option.dubbing,
                    metaContent = { color ->
                        TvDubbingMetaRow(
                            views = option.views.formatCompactCount(),
                            episodeCount = option.episodeCount,
                            sourceNames = option.sourceNames,
                            contentColor = color,
                        )
                    },
                    selected = false,
                    enabled = true,
                    accentLabel = true,
                    modifier = if (index == 0) Modifier.focusRequester(firstFocusRequester) else Modifier,
                    logoName = option.dubbing,
                    logoRes = dubbingLogoRes(option.dubbing),
                    logoFallbackIcon = Icons.Filled.Mic,
                    onExitDown = null,
                    onClick = { onSelect(option) },
                )
            }
        }
        TvControlButton(
            onClick = onCancel,
            onFocused = onInteraction,
        ) { color ->
            Text(
                text = stringResource(R.string.player_next_episode_dialog_cancel),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}
