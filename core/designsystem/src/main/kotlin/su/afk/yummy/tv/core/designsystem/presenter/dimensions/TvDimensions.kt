package su.afk.yummy.tv.core.designsystem.presenter.dimensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize

object TvScreenPadding {
    val Horizontal = 32.dp
    val Vertical = 32.dp
}

data class PosterCardDimensions(
    val width: Dp,
    val posterHeight: Dp,
)

@Composable
fun currentMobilePosterWidth(): Dp = LocalPosterCardSize.current.mobilePosterWidth

@Composable
fun currentTvTitleCardDimensions(): PosterCardDimensions =
    LocalPosterCardSize.current.tvTitleCardDimensions

@Composable
fun currentTvHomeFeedCardDimensions(): PosterCardDimensions =
    LocalPosterCardSize.current.tvHomeFeedCardDimensions

private val PosterCardSize.mobilePosterWidth: Dp
    get() = when (this) {
        PosterCardSize.COMPACT -> 104.dp
        PosterCardSize.STANDARD -> 140.dp
        PosterCardSize.LARGE -> 168.dp
    }

private val PosterCardSize.tvTitleCardDimensions: PosterCardDimensions
    get() = when (this) {
        PosterCardSize.COMPACT -> PosterCardDimensions(width = 144.dp, posterHeight = 198.dp)
        PosterCardSize.STANDARD -> PosterCardDimensions(width = 172.dp, posterHeight = 236.dp)
        PosterCardSize.LARGE -> PosterCardDimensions(width = 200.dp, posterHeight = 274.dp)
    }

private val PosterCardSize.tvHomeFeedCardDimensions: PosterCardDimensions
    get() = when (this) {
        PosterCardSize.COMPACT -> PosterCardDimensions(width = 152.dp, posterHeight = 194.dp)
        PosterCardSize.STANDARD -> PosterCardDimensions(width = 180.dp, posterHeight = 230.dp)
        PosterCardSize.LARGE -> PosterCardDimensions(width = 208.dp, posterHeight = 266.dp)
    }
