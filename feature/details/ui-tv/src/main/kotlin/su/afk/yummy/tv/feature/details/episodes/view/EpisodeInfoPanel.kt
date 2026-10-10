package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeInfo
import su.afk.yummy.tv.feature.details.presentation.R

/** Название и описание серии, на которой сейчас фокус в сетке. */
@Composable
internal fun EpisodeInfoPanel(
    episode: String,
    info: AnimeEpisodeInfo?,
    modifier: Modifier = Modifier,
) {
    // Высота по контенту: у серий без описания панель схлопывается, пустоты на экране нет
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.details_episode_number, episode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
        )
        info?.title?.takeIf { it.isNotBlank() }?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        info?.description?.takeIf { it.isNotBlank() }?.let { description ->
            // Скроллить текст с пульта нельзя — ограничиваем строками.
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
