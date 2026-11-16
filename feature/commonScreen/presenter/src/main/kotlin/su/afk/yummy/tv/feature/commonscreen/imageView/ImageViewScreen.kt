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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import su.afk.yummy.tv.core.model.text.UiText
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.error.asString
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
    var toastText by remember { mutableStateOf<UiText?>(null) }
    val isTelevision = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val context = LocalContext.current

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
    LaunchedEffect(effect) {
        effect.collect { e ->
            when (e) {
                is ImageViewState.Effect.ShowToast -> toastText = e.text
            }
        }
    }
    val toastMessage = toastText?.asString()
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            toastText = null
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
                contentDescription = stringResource(CoreR.string.common_back),
                tint = Color.White,
            )
        }

        if (!isTelevision) {
            IconButton(
                onClick = { onEvent(ImageViewState.Event.Download) },
                enabled = !state.isDownloading,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.download),
                    tint = Color.White,
                )
            }
        }
    }
}
