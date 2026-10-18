package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus

/** Статус выхода тайтла: цветная точка и подпись на тёмной капсуле — читается и поверх постера. */
@Composable
fun ReleaseStatusChip(
    status: AnimeReleaseStatus,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    dotSize: Dp = 6.dp,
) {
    val color = status.color()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(color, CircleShape),
        )
        Text(
            text = stringResource(status.labelRes()),
            style = textStyle,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

private fun AnimeReleaseStatus.labelRes(): Int = when (this) {
    AnimeReleaseStatus.ONGOING -> R.string.core_release_status_ongoing
    AnimeReleaseStatus.RELEASED -> R.string.core_release_status_released
    AnimeReleaseStatus.ANNOUNCEMENT -> R.string.core_release_status_announcement
}

private fun AnimeReleaseStatus.color(): Color = when (this) {
    AnimeReleaseStatus.ONGOING -> YummySemanticColors.ReleaseOngoing
    AnimeReleaseStatus.RELEASED -> YummySemanticColors.ReleaseReleased
    AnimeReleaseStatus.ANNOUNCEMENT -> YummySemanticColors.ReleaseAnnouncement
}
