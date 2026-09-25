package su.afk.yummy.tv.feature.commonscreen.imageView

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.commonscreen.R
import su.afk.yummy.tv.feature.commonscreen.imageView.view.NavigationControls
import su.afk.yummy.tv.feature.commonscreen.imageView.view.ThumbnailStrip
import su.afk.yummy.tv.feature.commonscreen.imageView.view.ZoomableImage

@Composable
internal fun ImageViewScreen(
    state: ImageViewState.State,
    effect: Flow<ImageViewState.Effect>,
    onEvent: (ImageViewState.Event) -> Unit,
) {
    val multipleImages = state.images.size > 1
    val pagerState = rememberPagerState(initialPage = state.selectedIndex) { state.images.size }
    val keyFocusRequester = remember { FocusRequester() }

    // Выбранный индекс — в ViewModel: пейджер догоняет его (стрелки, миниатюры, пульт),
    // а свайп сообщает новый индекс обратно.
    LaunchedEffect(state.selectedIndex) {
        if (pagerState.currentPage != state.selectedIndex) {
            pagerState.animateScrollToPage(state.selectedIndex)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != state.selectedIndex) onEvent(ImageViewState.Event.SelectIndex(page))
        }
    }
    // Без фокуса на корне клавиши пульта и клавиатуры сюда не доходят.
    LaunchedEffect(Unit) { keyFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(keyFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionRight -> {
                        onEvent(ImageViewState.Event.Next)
                        true
                    }

                    Key.DirectionLeft -> {
                        onEvent(ImageViewState.Event.Previous)
                        true
                    }

                    else -> false
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> state.images[page] },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (multipleImages) 80.dp else 0.dp),
        ) { page ->
            ZoomableImage(url = state.images[page])
        }

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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
            )
        }

        IconButton(
            onClick = { onEvent(ImageViewState.Event.Back) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
            )
        }
    }
}
