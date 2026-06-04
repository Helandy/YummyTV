package su.afk.yummy.tv.feature.commonscreen.imageView

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.commonscreen.R
import su.afk.yummy.tv.feature.commonscreen.imageView.view.NavigationControls
import su.afk.yummy.tv.feature.commonscreen.imageView.view.ThumbnailStrip

@Composable
internal fun ImageViewScreen(
    state: ImageViewState.State,
    effect: Flow<ImageViewState.Effect>,
    onEvent: (ImageViewState.Event) -> Unit,
) {
    val multipleImages = state.images.size > 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionRight -> { onEvent(ImageViewState.Event.Next); true }
                    Key.DirectionLeft -> { onEvent(ImageViewState.Event.Previous); true }
                    Key.Back, Key.Escape -> { onEvent(ImageViewState.Event.Back); true }
                    else -> false
                }
            },
    ) {
        AsyncImage(
            model = state.currentImage,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (multipleImages) 80.dp else 0.dp),
        )

        if (multipleImages) {
            NavigationControls(
                hasPrevious = state.hasPrevious,
                hasNext = state.hasNext,
                onPrevious = { onEvent(ImageViewState.Event.Previous) },
                onNext = { onEvent(ImageViewState.Event.Next) },
                modifier = Modifier.align(Alignment.Center),
            )

            ThumbnailStrip(
                images = state.images,
                selectedIndex = state.selectedIndex,
                onSelect = { onEvent(ImageViewState.Event.SelectIndex(it)) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
            )
        }

        IconButton(
            onClick = { onEvent(ImageViewState.Event.Back) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
            )
        }
    }
}
