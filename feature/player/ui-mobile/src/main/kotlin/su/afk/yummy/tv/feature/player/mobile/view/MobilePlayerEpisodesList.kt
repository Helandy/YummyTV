package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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

private val THUMBNAIL_WIDTH = 148.dp
private val THUMBNAIL_SHAPE = RoundedCornerShape(8.dp)

/** Прокручиваемый список серий под плеером в портрете: превью, номер, прогресс; текущая серия подсвечена. */
@Composable
fun MobilePlayerEpisodesList(
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
    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(episodes, key = { _, item -> lazyKey("player_episode", item.index) }) { _, item ->
                EpisodeRow(
                    item = item,
                    animeId = animeId,
                    posterUrl = posterUrl,
                    selected = item.index == currentEpisodeIndex,
                    onClick = { onEpisodeSelected(item.index) },
                )
            }
            item(key = "player_episodes_bottom_inset") {
                Box(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    item: PlayerEpisodeItem,
    animeId: Int,
    posterUrl: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(THUMBNAIL_WIDTH)
                .aspectRatio(16f / 9f)
                .clip(THUMBNAIL_SHAPE)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.player_episode_number, item.number),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
