package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.overline
import su.afk.yummy.tv.core.designsystem.utils.dubbingDisplayName
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
internal fun TvPlayerInfoBar(
    visible: Boolean,
    animeTitle: String,
    episode: String,
    dubbing: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && (animeTitle.isNotBlank() || episode.isNotBlank() || dubbing.isNotBlank()),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 16.dp, start = 16.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (animeTitle.isNotBlank() || episode.isNotBlank()) {
                val infoText = when {
                    animeTitle.isNotBlank() && episode.isNotBlank() -> stringResource(
                        R.string.player_info_title_episode,
                        animeTitle,
                        episode,
                    )
                    animeTitle.isNotBlank() -> animeTitle
                    else -> stringResource(R.string.player_episode_number, episode)
                }
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            if (dubbing.isNotBlank()) {
                Text(
                    text = dubbing.dubbingDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

private val TvFlagFillerColor = Color(0xFFFFC107)
private val TvFlagRecapColor = Color(0xFF90CAF9)

/** Правый верхний угол: имя плеера и под ним пометка текущей серии («Филлер»/«Рекап»). */
@Composable
internal fun TvPlayerNameBadge(
    visible: Boolean,
    playerName: String,
    episodeFlags: EpisodeFlags?,
    modifier: Modifier = Modifier,
) {
    val flagLabel = when {
        episodeFlags == null -> null
        episodeFlags.filler -> stringResource(R.string.player_episode_flag_filler) to TvFlagFillerColor
        episodeFlags.recap -> stringResource(R.string.player_episode_flag_recap) to TvFlagRecapColor
        else -> null
    }
    AnimatedVisibility(
        visible = visible && (playerName.isNotBlank() || flagLabel != null),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 16.dp, end = 16.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (playerName.isNotBlank()) {
                Text(
                    text = playerName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.overline(),
                    color = Color.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.90f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            if (flagLabel != null) {
                Text(
                    text = flagLabel.first,
                    style = MaterialTheme.typography.labelMedium,
                    color = flagLabel.second,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}
