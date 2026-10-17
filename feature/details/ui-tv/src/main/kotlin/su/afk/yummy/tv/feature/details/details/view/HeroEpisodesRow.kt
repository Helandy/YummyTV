package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.utils.formatAiredProgress
import su.afk.yummy.tv.feature.details.utils.formatNextEpisodeRelease

@Composable
internal fun HeroEpisodesRow(details: AnimeDetails) {
    val episodes = details.episodes ?: return
    episodes.formatAiredProgress(details.status)?.let { episodeProgress ->
        Text(
            text = episodeProgress,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
        )
    }
    episodes.formatNextEpisodeRelease()?.let { nextEpisode ->
        Text(
            text = nextEpisode,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
