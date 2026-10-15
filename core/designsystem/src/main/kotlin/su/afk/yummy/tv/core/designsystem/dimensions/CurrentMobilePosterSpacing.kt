package su.afk.yummy.tv.core.designsystem.dimensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterCardSize

@Composable
fun currentMobilePosterSpacing(): Dp = LocalPosterCardSize.current.mobileSpacing

private val PosterCardSize.mobileSpacing: Dp
    get() = when (this) {
        PosterCardSize.COMPACT -> 6.dp
        PosterCardSize.STANDARD -> 8.dp
        PosterCardSize.LARGE -> 12.dp
    }
