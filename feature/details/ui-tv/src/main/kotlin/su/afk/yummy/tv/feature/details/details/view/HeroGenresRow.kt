package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.model.anime.AnimeDetails

@Composable
internal fun HeroGenresRow(details: AnimeDetails) {
    val genres = details.genres.take(5).joinToString(" • ") { it.title }
    if (genres.isBlank()) return
    Text(
        text = genres,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
