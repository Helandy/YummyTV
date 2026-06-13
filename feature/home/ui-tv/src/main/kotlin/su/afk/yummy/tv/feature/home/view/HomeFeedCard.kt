package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvHomeFeedCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

@Composable
internal fun HomeFeedCard(
    item: HomeFeedItem,
    onClick: () -> Unit,
    onFocused: (displayId: Int, animeId: Int?) -> Unit,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    focusedScale: Float = 1.04f,
    forceFocused: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val active = isFocused || forceFocused
    val cardDimensions = currentTvHomeFeedCardDimensions()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val animeId = (item.action as? HomeFeedItemAction.OpenSeries)?.seriesId

    val shape = RoundedCornerShape(8.dp)

    Card(
        modifier = modifier
            .width(cardDimensions.width)
            .onFocusChanged { focusState ->
                val focused = focusState.isFocused || focusState.hasFocus
                if (focused && !isFocused) onFocused(item.id, animeId)
                isFocused = focused
            }
            .focusable(interactionSource = interactionSource)
            .focusProperties {
                upFocusRequester?.let { up = it }
                mainMenuFocusRequester?.let { left = it }
                downFocusRequester?.let { down = it }
            }
            .tvFocusableClick(
                onClick = onClick,
                shape = shape,
                interactionSource = interactionSource,
                focusedScale = focusedScale,
            )
            .border(
                width = 3.dp,
                color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardDimensions.posterHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = item.posterUrl(LocalPosterQuality.current)

                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                if (imageUrl == null) {
                    Text(
                        text = item.title.take(1),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                    )
                }

                item.rating?.let { rating ->
                    RatingBadge(
                        rating = rating,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

internal fun HomeFeedItem.posterUrl(quality: PosterQuality): String? = when (quality) {
    PosterQuality.LOW -> poster?.medium ?: poster?.big ?: poster?.fullsize ?: poster?.small
    PosterQuality.STANDARD -> poster?.big ?: poster?.medium ?: poster?.fullsize ?: poster?.small
    PosterQuality.MEGA -> poster?.mega ?: poster?.big ?: poster?.medium ?: poster?.fullsize ?: poster?.small
    PosterQuality.HIGH -> poster?.fullsize ?: poster?.mega ?: poster?.big ?: poster?.medium ?: poster?.small
}
