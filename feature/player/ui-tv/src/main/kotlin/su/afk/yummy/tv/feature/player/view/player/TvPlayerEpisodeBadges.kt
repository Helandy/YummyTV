package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeItem
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress
import su.afk.yummy.tv.feature.player.presentation.R

private val FillerColor = Color(0xFFFFC107)
private val RecapColor = Color(0xFF90CAF9)
private val BadgeBackground = Color.Black.copy(alpha = 0.72f)

/**
 * Значки серии в правом верхнем углу превью, как в списке серий тайтла: филлер/рекап, тайминг просмотра,
 * «просмотрено» (глаз) или «начата» (точка). Цвет текста задан явно: на ТВ он по умолчанию чёрный.
 */
@Composable
internal fun TvPlayerEpisodeBadges(
    item: PlayerEpisodeItem,
    modifier: Modifier = Modifier,
) {
    val timing = when (val progress = item.progress) {
        PlayerEpisodeProgress.None -> item.durationLabel
        is PlayerEpisodeProgress.InProgress -> progress.label
        is PlayerEpisodeProgress.Watched -> progress.label
    }
    val flag = item.flags?.let { flags ->
        when {
            flags.filler -> stringResource(R.string.player_episode_flag_filler) to FillerColor
            flags.recap -> stringResource(R.string.player_episode_flag_recap) to RecapColor
            else -> null
        }
    }
    if (timing == null && flag == null && item.progress == PlayerEpisodeProgress.None) return
    // Тайминг — справа снизу (над полоской прогресса), пометки и статус просмотра — справа сверху.
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (flag != null) BadgeText(text = flag.first, color = flag.second)
            when (item.progress) {
                PlayerEpisodeProgress.None -> Unit
                is PlayerEpisodeProgress.InProgress -> Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(YummySemanticColors.InProgress, CircleShape),
                )

                is PlayerEpisodeProgress.Watched -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(BadgeBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (timing != null) {
            BadgeText(
                text = timing,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun BadgeText(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        modifier = modifier
            .background(BadgeBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
