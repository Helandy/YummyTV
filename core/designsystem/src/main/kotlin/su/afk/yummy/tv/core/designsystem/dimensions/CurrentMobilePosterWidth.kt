package su.afk.yummy.tv.core.designsystem.dimensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterCardSize

@Composable
fun currentMobilePosterWidth(): Dp = LocalPosterCardSize.current.mobilePosterWidth

private val PosterCardSize.mobilePosterWidth: Dp
    get() = when (this) {
        PosterCardSize.COMPACT -> 88.dp
        PosterCardSize.STANDARD -> 100.dp
        PosterCardSize.LARGE -> 140.dp
    }
