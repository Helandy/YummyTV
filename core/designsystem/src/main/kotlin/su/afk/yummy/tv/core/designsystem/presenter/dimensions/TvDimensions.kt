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

object TvCardSpacing {
    val Horizontal = 12.dp
    val Vertical = 12.dp
}

const val TITLE_POSTER_ASPECT_RATIO = 570f / 800f

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
        PosterCardSize.COMPACT -> posterCardDimensions(width = 144.dp)
        PosterCardSize.STANDARD -> posterCardDimensions(width = 172.dp)
        PosterCardSize.LARGE -> posterCardDimensions(width = 200.dp)
    }

private val PosterCardSize.tvHomeFeedCardDimensions: PosterCardDimensions
    get() = when (this) {
        PosterCardSize.COMPACT -> posterCardDimensions(width = 152.dp)
        PosterCardSize.STANDARD -> posterCardDimensions(width = 180.dp)
        PosterCardSize.LARGE -> posterCardDimensions(width = 208.dp)
    }

private fun posterCardDimensions(width: Dp): PosterCardDimensions = PosterCardDimensions(
    width = width,
    posterHeight = width / TITLE_POSTER_ASPECT_RATIO,
)
