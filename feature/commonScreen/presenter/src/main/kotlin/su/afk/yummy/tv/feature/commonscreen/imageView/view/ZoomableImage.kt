package su.afk.yummy.tv.feature.commonscreen.imageView.view

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import coil3.compose.AsyncImage

/**
 * Картинка с масштабированием щипком (1×–5×) и сдвигом увеличенной картинки. Пока масштаб 1×,
 * жесты не перехватываются — горизонтальный свайп листает пейджер.
 */
@Composable
internal fun ZoomableImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }
    var viewportCenter by remember(url) { mutableStateOf(Offset.Zero) }
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

    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
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

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
