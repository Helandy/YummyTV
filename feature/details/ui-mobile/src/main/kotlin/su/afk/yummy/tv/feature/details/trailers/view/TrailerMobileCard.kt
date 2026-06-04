package su.afk.yummy.tv.feature.details.trailers.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.trailers.*
import su.afk.yummy.tv.feature.details.view.DetailsMediaCard
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold
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
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailer.externalWatchUrl))
            runCatching { context.startActivity(intent) }
        },
    )
}
