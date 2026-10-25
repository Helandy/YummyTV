package su.afk.yummy.tv.core.designsystem.dimensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterCardSize

@Composable
fun currentMobilePosterTitleStyle(): TextStyle = when (LocalPosterCardSize.current) {
    PosterCardSize.COMPACT -> MaterialTheme.typography.bodySmall
    PosterCardSize.STANDARD,
    PosterCardSize.LARGE,
    -> MaterialTheme.typography.bodyMedium
}

@Composable
fun currentMobilePosterTitleWeight(): FontWeight = when (LocalPosterCardSize.current) {
    PosterCardSize.COMPACT -> FontWeight.Normal
    PosterCardSize.STANDARD,
    PosterCardSize.LARGE,
    -> FontWeight.SemiBold
}

@Composable
fun currentMobilePosterTextPadding(): Dp = when (LocalPosterCardSize.current) {
    PosterCardSize.COMPACT -> 6.dp
    PosterCardSize.STANDARD,
    PosterCardSize.LARGE,
    -> 8.dp
}

/** Множитель размера плашек на постере (рейтинг, год): крупные карточки получают пропорционально крупные плашки. */
@Composable
fun currentMobilePosterBadgeScale(): Float = when (LocalPosterCardSize.current) {
    PosterCardSize.COMPACT,
    PosterCardSize.STANDARD,
    -> 1f
    PosterCardSize.LARGE -> 1.35f
}
