package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.overline
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.core.designsystem.utils.dubbingDisplayName
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

@Composable
internal fun MobilePlayerTopBar(
    title: String,
    episode: String,
    dubbing: String,
    playerName: String,
    episodeFlags: EpisodeFlags?,
    onBack: () -> Unit,
    onLockClick: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.28f))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(DesignSystemR.string.common_back),
                        tint = Color.White,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (episode.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.player_mobile_episode_number, episode),
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (dubbing.isNotBlank()) {
                        Text(
                            text = dubbing.dubbingDisplayName(),
                            color = Color.White.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    if (playerName.isNotBlank()) {
                        Text(
                            text = playerName.uppercase(),
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.overline(),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.88f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                    MobilePlayerEpisodeFlagLabel(
                        flags = episodeFlags,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    // Круглая подложка с обводкой, как у кнопок управления внизу: голые точки
                    // терялись на светлом кадре.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        IconButton(
                            onClick = onLockClick,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.36f))
                                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                        ) {
                            Icon(
                                Icons.Filled.LockOpen,
                                contentDescription = stringResource(R.string.player_mobile_lock),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val FlagFillerColor = Color(0xFFFFC107)
private val FlagRecapColor = Color(0xFF90CAF9)

/** Плашка «Филлер»/«Рекап» текущей серии под именем плеера. */
@Composable
private fun MobilePlayerEpisodeFlagLabel(flags: EpisodeFlags?, modifier: Modifier = Modifier) {
    val (text, color) = when {
        flags == null -> return
        flags.filler -> stringResource(R.string.player_episode_flag_filler) to FlagFillerColor
        flags.recap -> stringResource(R.string.player_episode_flag_recap) to FlagRecapColor
        else -> return
    }
    Text(
        text = text,
        color = color,
        maxLines = 1,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
