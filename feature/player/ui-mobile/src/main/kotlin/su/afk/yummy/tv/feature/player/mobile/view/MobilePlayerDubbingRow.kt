package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.LogoBadge
import su.afk.yummy.tv.core.designsystem.utils.dubbingLogoRes
import su.afk.yummy.tv.feature.player.presentation.R

/** Строка озвучки как в шторке выбора: логотип, акцентное название, просмотры, серии и плееры. */
@Composable
internal fun MobilePlayerDubbingRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    views: Int,
    episodeCount: Int,
    sourceNames: String,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    }
    // Названия озвучек — акцентным цветом, как в шторках выбора озвучки и скачивания.
    // Выбранная отличается подложкой, жирностью и галочкой.
    val textColor = MaterialTheme.colorScheme.primary
        .let { if (enabled) it else it.copy(alpha = 0.42f) }
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LogoBadge(logoRes = dubbingLogoRes(label), size = 32.dp, fallbackIcon = Icons.Filled.Mic)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (enabled) {
                MobilePlayerDubbingMeta(
                    views = views,
                    episodeCount = episodeCount,
                    sourceNames = sourceNames,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.player_episode_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = metaColor,
                )
            }
        }
    }
}
