package su.afk.yummy.tv.feature.account.mobile.view

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.feature.account.mobile.profileedit.utils.loadOrientedPreview
import su.afk.yummy.tv.feature.account.presentation.R
import kotlin.math.max

private const val MAX_ZOOM = 6f

/** Full-screen pan/zoom cropper. Returns the chosen area as fractions (0..1) of the oriented image. */
@Composable
fun ProfileImageCropDialog(
    uri: Uri,
    kind: ProfileImageKind,
    onCropped: (RectF) -> Unit,
    onLoadFailed: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = loadOrientedPreview(context, uri)
        if (bitmap == null) onLoadFailed()
    }
    val aspect = when (kind) {
        ProfileImageKind.AVATAR -> 1f
        ProfileImageKind.BANNER -> 1370f / 170f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            var area by remember { mutableStateOf(IntSize.Zero) }
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val bmp = bitmap

            // Crop frame, centered in the available area with a 16dp margin.
            val marginPx = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() }
            val frameWidth = minOf(area.width - 2 * marginPx, (area.height - 2 * marginPx) * aspect)
            val frameHeight = frameWidth / aspect
            val minScale = if (bmp != null && frameWidth > 0f) {
                max(frameWidth / bmp.width, frameHeight / bmp.height)
            } else {
                1f
            }

            fun clamp(s: Float, o: Offset): Offset {
                if (bmp == null) return o
                val maxX = (bmp.width * s - frameWidth) / 2f
                val maxY = (bmp.height * s - frameHeight) / 2f
                return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
            }

            LaunchedEffect(bmp, area, aspect) {
                if (bmp != null && frameWidth > 0f) {
                    scale = minScale
                    offset = Offset.Zero
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                Text(stringResource(R.string.profile_crop_title), color = Color.White)
                TextButton(
                    enabled = bmp != null && frameWidth > 0f,
                    onClick = {
                        val b = bmp ?: return@TextButton
                        val cx = b.width / 2f - offset.x / scale
                        val cy = b.height / 2f - offset.y / scale
                        val w = frameWidth / scale
                        val h = frameHeight / scale
                        onCropped(
                            RectF(
                                ((cx - w / 2f) / b.width).coerceIn(0f, 1f),
                                ((cy - h / 2f) / b.height).coerceIn(0f, 1f),
                                ((cx + w / 2f) / b.width).coerceIn(0f, 1f),
                                ((cy + h / 2f) / b.height).coerceIn(0f, 1f),
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.profile_crop_apply)) }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged { area = it }
                    .pointerInput(bmp, frameWidth, frameHeight) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(minScale, minScale * MAX_ZOOM)
                            scale = newScale
                            offset = clamp(newScale, offset + pan)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (bmp == null) {
                    CircularProgressIndicator()
                } else {
                    val image = remember(bmp) { bmp.asImageBitmap() }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dstSize = IntSize((bmp.width * scale).toInt(), (bmp.height * scale).toInt())
                        val dstOffset = IntOffset(
                            (center.x + offset.x - dstSize.width / 2f).toInt(),
                            (center.y + offset.y - dstSize.height / 2f).toInt(),
                        )
                        drawImage(image, dstOffset = dstOffset, dstSize = dstSize)

                        val frameTopLeft = Offset(center.x - frameWidth / 2f, center.y - frameHeight / 2f)
                        val frame = androidx.compose.ui.geometry.Rect(frameTopLeft, Size(frameWidth, frameHeight))
                        val scrim = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
                            addRect(frame)
                        }
                        drawPath(scrim, Color.Black.copy(alpha = 0.6f))
                        drawRect(Color.White, frameTopLeft, Size(frameWidth, frameHeight), style = Stroke(2.dp.toPx()))
                    }
                }
            }

        }
    }
}
