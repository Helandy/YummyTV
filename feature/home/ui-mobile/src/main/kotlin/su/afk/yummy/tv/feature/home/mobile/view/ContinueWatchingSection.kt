@file:JvmName("MobileContinueWatchingSectionKt")

package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileProgressMediaCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.utils.KodikThumbnail
import su.afk.yummy.tv.core.utils.resolveContinueWatchingImageModel
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.home.mobile.R
import su.afk.yummy.tv.feature.home.mobile.utils.bestUrl
import su.afk.yummy.tv.feature.home.mobile.utils.episodeSubtitle
import su.afk.yummy.tv.feature.home.mobile.utils.timingSubtitle
import su.afk.yummy.tv.feature.home.mobile.utils.watchProgress

@Composable
internal fun ContinueWatchingSection(
    entries: List<HomeContinueWatchingItem>,
    onEntrySelected: (HomeContinueWatchingItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MobileSectionHeader(
            title = stringResource(R.string.home_mobile_continue_watching),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = entries,
                key = { index, entry ->
                    "${entry.animeId}-${entry.episode}-${entry.videoId}-${entry.updatedAt}-$index"
                },
            ) { _, entry ->
                val episodeTitle = if (entry.episode.isBlank()) {
                    stringResource(R.string.home_mobile_episode_unknown)
                } else {
                    stringResource(R.string.home_mobile_episode, entry.episode)
                }
                val imageModel = resolveContinueWatchingImageModel(
                    screenshotUrl = entry.screenshotUrl,
                    episodeUrl = entry.episodeUrl,
                    posterUrl = entry.poster.bestUrl(),
                    kodikThumbnailModel = ::KodikThumbnail,
                )
                MobileProgressMediaCard(
                    title = entry.animeTitle.ifBlank { episodeTitle },
                    imageModel = imageModel,
                    subtitle = entry.episodeSubtitle(),
                    trailingSubtitle = entry.timingSubtitle(),
                    progress = entry.watchProgress(),
                    onClick = { onEntrySelected(entry) },
                )
            }
        }
    }
}
