package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.utils.formatAiredProgress

@Composable
internal fun HeroEpisodesRow(details: AnimeDetails) {
    val episodeProgress = details.episodes?.formatAiredProgress(details.status) ?: return
    Text(
        text = episodeProgress,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
    )
}
