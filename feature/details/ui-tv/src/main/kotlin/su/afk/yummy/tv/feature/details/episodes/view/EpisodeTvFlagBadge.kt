package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.feature.details.presentation.R

private val FillerColor = Color(0xFFFFC107)
private val RecapColor = Color(0xFF90CAF9)

/** Пометка серии из MyAnimeList: «Филлер» важнее «Рекапа», если выставлены обе. */
@Composable
internal fun EpisodeTvFlagBadge(
    flags: EpisodeFlags,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when {
        flags.filler -> stringResource(R.string.details_episode_flag_filler) to FillerColor
        flags.recap -> stringResource(R.string.details_episode_flag_recap) to RecapColor
        else -> return
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
