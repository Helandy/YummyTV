package su.afk.yummy.tv.feature.details.trailers.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.view.DetailsMediaCard
import su.afk.yummy.tv.feature.details.view.DetailsPlayIcon

@Composable
internal fun TrailerMobileCard(
    number: Int,
    trailer: AnimeTrailer,
) {
    val context = LocalContext.current
    DetailsMediaCard(
        title = stringResource(R.string.details_mobile_trailer_number, number),
        subtitle = trailer.youtubeWatchUrl ?: trailer.iframeUrl,
        imageUrl = trailer.youtubeThumbnailUrl,
        leadingIcon = DetailsPlayIcon,
        mediaWeight = 0.56f,
        onClick = {
            context.openExternalUri(trailer.externalWatchUrl)
        },
    )
}
