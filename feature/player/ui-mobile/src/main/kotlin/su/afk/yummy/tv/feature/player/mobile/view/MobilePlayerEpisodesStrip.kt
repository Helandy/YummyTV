package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.player.common.utils.thumbnailModels
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeItem
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress
import su.afk.yummy.tv.feature.player.presentation.R

private val STRIP_CARD_WIDTH = 150.dp
private val STRIP_CARD_SHAPE = RoundedCornerShape(8.dp)

/** Горизонтальная лента серий внизу полноэкранного плеера (ландшафт): листается вместе с панелью управления. */
@Composable
fun MobilePlayerEpisodesStrip(
    episodes: ImmutableList<PlayerEpisodeItem>,
    currentEpisodeIndex: Int,
    animeId: Int,
    posterUrl: String,
    onEpisodeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentEpisodeIndex, episodes.size) {
        val position = episodes.indexOfFirst { it.index == currentEpisodeIndex }
        if (position >= 0) listState.scrollToItem(position)
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(episodes, key = { _, item -> lazyKey("player_episode_strip", item.index) }) { _, item ->
            StripCard(
                item = item,
                animeId = animeId,
                posterUrl = posterUrl,
                selected = item.index == currentEpisodeIndex,
                onClick = { onEpisodeSelected(item.index) },
            )
        }
    }
}

@Composable
private fun StripCard(
    item: PlayerEpisodeItem,
    animeId: Int,
    posterUrl: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .width(STRIP_CARD_WIDTH)
            .clip(STRIP_CARD_SHAPE)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(STRIP_CARD_SHAPE)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            val thumbnailModels = remember(item, animeId, posterUrl) { item.thumbnailModels(animeId, posterUrl) }
            if (thumbnailModels.isNotEmpty()) {
                FallbackAsyncImage(
                    models = thumbnailModels,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            when (val progress = item.progress) {
                is PlayerEpisodeProgress.InProgress -> LinearProgressIndicator(
                    progress = { progress.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = YummySemanticColors.InProgress,
                    trackColor = Color.Black.copy(alpha = 0.5f),
                )

                else -> Unit
            }
            MobilePlayerEpisodeBadges(
                item = item,
                modifier = Modifier.matchParentSize(),
            )
        }
        Text(
            text = stringResource(R.string.player_episode_number, item.number),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) primary else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
