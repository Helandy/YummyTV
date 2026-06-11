@file:JvmName("MobileContinueWatchingSectionKt")

package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileProgressMediaCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.feature.home.mobile.R
import su.afk.yummy.tv.feature.home.utils.episodeSubtitle
import su.afk.yummy.tv.feature.home.utils.resolveMobileContinueWatchingImage
import su.afk.yummy.tv.feature.home.utils.timingSubtitle
import su.afk.yummy.tv.feature.home.utils.watchProgress

@Composable
internal fun ContinueWatchingSection(
    entries: List<WatchProgressEntry>,
    onEntrySelected: (WatchProgressEntry) -> Unit,
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
            items(entries, key = { "${it.animeId}-${it.episode}-${it.videoId}" }) { entry ->
                val episodeTitle = stringResource(R.string.home_mobile_episode, entry.episode)
                val imageUrl by produceState<String?>(
                    null,
                    entry.screenshotUrl,
                    entry.episodeUrl,
                    entry.posterUrl
                ) {
                    value = entry.resolveMobileContinueWatchingImage()
                }
                MobileProgressMediaCard(
                    title = entry.animeTitle.ifBlank { episodeTitle },
                    imageUrl = imageUrl,
                    subtitle = entry.episodeSubtitle(),
                    trailingSubtitle = entry.timingSubtitle(),
                    progress = entry.watchProgress(),
                    onClick = { onEntrySelected(entry) },
                )
            }
        }
    }
}
