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
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileProgressMediaCard
import su.afk.yummy.tv.core.model.anime.urlsByQuality
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailModels
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailOf
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.home.mobile.utils.episodeSubtitle
import su.afk.yummy.tv.feature.home.mobile.utils.timingSubtitle
import su.afk.yummy.tv.feature.home.mobile.utils.watchProgress
import su.afk.yummy.tv.feature.home.presentation.R

@Composable
internal fun ContinueWatchingSection(
    entries: List<HomeContinueWatchingItem>,
    launchingAnimeId: Int?,
    onEntrySelected: (HomeContinueWatchingItem) -> Unit,
    onEntryLongClick: (HomeContinueWatchingItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MobileSectionHeader(
            title = stringResource(R.string.continue_watching),
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
                    stringResource(R.string.home_episode)
                } else {
                    stringResource(R.string.home_episode_number, entry.episode)
                }
                val imageModels = episodeThumbnailModels(
        thumbnail = episodeThumbnailOf(entry.animeId, entry.episode, entry.screenshotUrl, entry.episodeUrl),
        posterUrls = entry.poster.urlsByQuality(LocalPosterQuality.current),
    )
                MobileProgressMediaCard(
                    title = entry.animeTitle.ifBlank { episodeTitle },
                    imageModels = imageModels,
                    subtitle = entry.episodeSubtitle(),
                    trailingSubtitle = entry.timingSubtitle(),
                    progress = entry.watchProgress(),
                    isLoading = entry.animeId == launchingAnimeId,
                    placeholderText = entry.episode,
                    onLongClick = { onEntryLongClick(entry) },
                    onClick = { onEntrySelected(entry) },
                )
            }
        }
    }
}
