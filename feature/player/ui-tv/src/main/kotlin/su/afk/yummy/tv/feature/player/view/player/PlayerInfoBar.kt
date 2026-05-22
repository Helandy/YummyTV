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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.R

@Composable
internal fun PlayerInfoBar(
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
                    text = dubbing,
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

@Composable
internal fun PlayerNameBadge(
    visible: Boolean,
    playerName: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && playerName.isNotBlank(),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 16.dp, end = 16.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = playerName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.90f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
