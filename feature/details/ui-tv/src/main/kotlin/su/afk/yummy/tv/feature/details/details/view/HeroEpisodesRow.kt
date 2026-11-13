package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.utils.formatAiredProgress
import su.afk.yummy.tv.feature.details.utils.formatNextEpisodeRelease

/** Прогресс выхода серий и бейдж следующей серии в одну строку. */
@Composable
internal fun HeroEpisodesRow(details: AnimeDetails) {
    val episodes = details.episodes ?: return
    val progress = episodes.formatAiredProgress(details.status)
    val nextEpisode = episodes.formatNextEpisodeRelease()
    if (progress == null && nextEpisode == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        progress?.let { episodeProgress ->
            Text(
                text = episodeProgress,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }
        nextEpisode?.let { next ->
            val primary = MaterialTheme.colorScheme.primary
            Text(
                text = next,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = primary,
                modifier = Modifier
                    .background(primary.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}
