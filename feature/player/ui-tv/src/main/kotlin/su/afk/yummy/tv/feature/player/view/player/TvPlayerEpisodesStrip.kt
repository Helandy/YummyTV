package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import kotlinx.collections.immutable.ImmutableList
import su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.player.common.utils.thumbnailModels
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeItem
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress
import su.afk.yummy.tv.feature.player.presentation.R

/** Высота ленты: она перекрывает ряд управления, видео остаётся на месте. */
private val STRIP_HEIGHT = 212.dp

private val CARD_WIDTH = 200.dp
private val CARD_SHAPE = RoundedCornerShape(10.dp)

/** Горизонтальная лента серий под видео: превью, номер, прогресс; текущая серия получает фокус при открытии. */
@Composable
internal fun TvPlayerEpisodesStrip(
    visible: Boolean,
    episodes: ImmutableList<PlayerEpisodeItem>,
    currentEpisodeIndex: Int,
    selectedFocusRequester: FocusRequester,
    animeId: Int,
    posterUrl: String,
    onEpisodeSelected: (Int) -> Unit,
    onExitUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && episodes.isNotEmpty(),
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val listState = rememberLazyListState()
        val currentPosition = episodes.indexOfFirst { it.index == currentEpisodeIndex }.coerceAtLeast(0)
        LaunchedEffect(visible, episodes.size, currentPosition) {
            if (visible) listState.scrollToItem(currentPosition)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(STRIP_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to Color.Black.copy(alpha = 0.94f),
                        1f to Color.Black.copy(alpha = 0.96f),
                    ),
                )
                .padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.player_episodes_list_title),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(horizontal = 48.dp),
            )
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(episodes, key = { _, item -> lazyKey("player_episode", item.index) }) { _, item ->
                    val selected = item.index == currentEpisodeIndex
                    EpisodeCard(
                        item = item,
                        animeId = animeId,
                        posterUrl = posterUrl,
                        selected = selected,
                        modifier = if (selected) Modifier.focusRequester(selectedFocusRequester) else Modifier,
                        onExitUp = onExitUp,
                        onClick = { onEpisodeSelected(item.index) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeCard(
    item: PlayerEpisodeItem,
    animeId: Int,
    posterUrl: String,
    selected: Boolean,
    onExitUp: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .tvPlayerFocusScale(focused)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    onExitUp()
                    true
                } else {
                    false
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(CARD_SHAPE)
                .background(Color.White.copy(alpha = 0.10f))
                .border(
                    width = if (focused) 3.dp else if (selected) 2.dp else 0.dp,
                    color = if (focused) primary else Color.White.copy(alpha = 0.86f),
                    shape = CARD_SHAPE,
                ),
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
            TvPlayerEpisodeBadges(
                item = item,
                modifier = Modifier.matchParentSize(),
            )
        }
        Text(
            text = stringResource(R.string.player_episode_number, item.number),
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) primary else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
