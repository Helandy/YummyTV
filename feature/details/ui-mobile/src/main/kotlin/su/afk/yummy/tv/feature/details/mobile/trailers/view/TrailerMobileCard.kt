package su.afk.yummy.tv.feature.details.mobile.trailers.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.feature.details.mobile.view.DetailsMediaCard
import su.afk.yummy.tv.feature.details.mobile.view.DetailsPlayIcon
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun TrailerMobileCard(
    number: Int,
    trailer: AnimeTrailer,
    onClick: () -> Unit,
) {
    DetailsMediaCard(
        title = stringResource(R.string.details_trailer_number, number),
        subtitle = trailer.youtubeWatchUrl ?: trailer.iframeUrl,
        imageModel = trailer.youtubeThumbnailUrl,
        leadingIcon = DetailsPlayIcon,
        mediaWeight = 0.56f,
        onClick = onClick,
    )
}
