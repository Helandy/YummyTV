package su.afk.yummy.tv.feature.details.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerContainer
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerItem
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberFocusRestorerState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun TrailerTab(
    trailers: List<AnimeTrailer>,
    onTrailerSelected: (String) -> Unit,
) {
    if (trailers.isEmpty()) {
        Text(
            text = stringResource(R.string.details_trailer_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        return
    }

    val restorerState = rememberFocusRestorerState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.details_trailers),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.focusRestorerContainer(restorerState),
        ) {
            itemsIndexed(trailers) { index, trailer ->
                TrailerItem(
                    number = index + 1,
                    trailer = trailer,
                    onPlay = { onTrailerSelected(trailer.iframeUrl) },
                    thumbnailModifier = Modifier.focusRestorerItem(index, restorerState),
                )
            }
        }
    }
}

@Composable
private fun TrailerItem(
    number: Int,
    trailer: AnimeTrailer,
    onPlay: () -> Unit,
    thumbnailModifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (trailer.youtubeWatchUrl != null) {
            YoutubeButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailer.youtubeWatchUrl))
                    context.startActivity(intent)
                },
            )
        }

        TrailerThumbnail(
            number = number,
            thumbnailUrl = trailer.youtubeThumbnailUrl,
            onClick = onPlay,
            modifier = thumbnailModifier,
        )
    }
}

@Composable
private fun TrailerThumbnail(
    number: Int,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Surface(
        modifier = modifier
            .width(256.dp)
            .height(144.dp)
            .tvFocusableClick(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = shape,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // dark scrim so play icon is always visible
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = if (focused) 0.25f else 0.45f),
                ) {}
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White,
                )
                Text(
                    text = stringResource(R.string.details_trailer_number, number),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun YoutubeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .width(256.dp)
            .tvFocusableClick(onClick = onClick, shape = shape),
        shape = shape,
        color = Color(0xFFFF0000),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.details_watch_youtube),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}
