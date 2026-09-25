package su.afk.yummy.tv.feature.commonscreen.imageView.view

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import su.afk.yummy.tv.feature.commonscreen.R

/**
 * Картинка с масштабированием щипком (1×–5×) и сдвигом увеличенной картинки. Пока масштаб 1×,
 * жесты не перехватываются — горизонтальный свайп листает пейджер.
 *
 * Пока грузится полная картинка, показывается её миниатюра из memory cache (её уже загрузила
 * лента миниатюр) и индикатор. Ошибку один раз повторяем сами: CDN скриншотов редиректит на
 * случайное зеркало, и мёртвое зеркало даёт connect timeout, — дальше кнопка «Повторить».
 */
@Composable
internal fun ZoomableImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }
    var viewportCenter by remember(url) { mutableStateOf(Offset.Zero) }
    var attempt by remember(url) { mutableIntStateOf(0) }
    var loadState by remember(url, attempt) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val previousScale = scale
        val nextScale = (previousScale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        val appliedZoom = nextScale / previousScale
        val focalPoint = centroid - viewportCenter
        scale = nextScale
        offset = if (nextScale == MIN_SCALE) {
            Offset.Zero
        } else {
            (offset - focalPoint) * appliedZoom + focalPoint + panChange
        }
    }
    val context = LocalPlatformContext.current
    val request = remember(context, url) {
        ImageRequest.Builder(context)
            .data(url)
            .placeholderMemoryCacheKey(url)
            .build()
    }
    val isError = loadState is AsyncImagePainter.State.Error

    LaunchedEffect(isError) {
        if (isError && attempt < AUTO_RETRY_COUNT) attempt++
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Новый key пересоздаёт painter — только так тот же запрос уходит в сеть заново.
        key(attempt) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onState = { loadState = it },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportCenter = Offset(it.width / 2f, it.height / 2f) }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    )
                    .transformable(transformState, canPan = { scale > MIN_SCALE }),
            )
        }

        when {
            loadState is AsyncImagePainter.State.Loading ->
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )

            isError && attempt >= AUTO_RETRY_COUNT ->
                TextButton(
                    onClick = { attempt++ },
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text(text = stringResource(R.string.image_view_retry), color = Color.White)
                }
        }
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val AUTO_RETRY_COUNT = 1
