package su.afk.yummy.tv.feature.home.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.presenter.components.MarqueeTitleText
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.HomeFeedCardDefaults
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalShowScreenshotsOnFocus
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

@Composable
internal fun HomeFeedCard(
    item: HomeFeedItem,
    preview: AnimePreview?,
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
    val showScreenshots = LocalShowScreenshotsOnFocus.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val animeId = (item.action as? HomeFeedItemAction.OpenSeries)?.seriesId

    val screenshots = preview?.screenshotUrls.orEmpty()
    var slideIndex by remember(item.id) { mutableIntStateOf(0) }

    LaunchedEffect(active, showScreenshots, screenshots) {
        if (active && showScreenshots && screenshots.size > 1) {
            while (true) {
                delay(2000)
                slideIndex = (slideIndex + 1) % screenshots.size
            }
        } else {
            slideIndex = 0
        }
    }

    val shape = RoundedCornerShape(8.dp)

    Card(
        modifier = modifier
            .width(HomeFeedCardDefaults.Width)
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
                    .height(HomeFeedCardDefaults.PosterHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = when {
                    active && showScreenshots && screenshots.isNotEmpty() -> screenshots[slideIndex]
                    else -> item.posterUrl(LocalPosterQuality.current)
                }

                Crossfade(
                    targetState = imageUrl,
                    animationSpec = tween(400),
                    label = "card_image",
                ) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (imageUrl == null) {
                    Text(
                        text = item.title.take(1),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                    )
                }

                item.rating?.let { rating ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(9.dp),
                        )
                        Text(
                            text = " %.2f".format(rating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                MarqueeTitleText(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    minLines = 2,
                    maxLines = 2,
                    isFocused = active,
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
