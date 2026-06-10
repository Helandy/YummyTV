package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import su.afk.yummy.tv.feature.player.model.MobileSeekDirection
import kotlin.math.sqrt

@Composable
internal fun MobilePlayerGestureLayer(
    enabled: Boolean,
    onTap: () -> Unit,
    onDoubleTap: (MobileSeekDirection) -> Unit,
    onTransform: (centroid: Offset, pan: Offset, zoomChange: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled, onTap, onDoubleTap) {
                if (!enabled) return@pointerInput

                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { offset ->
                        val direction = if (offset.x < size.width / 2f) {
                            MobileSeekDirection.Backward
                        } else {
                            MobileSeekDirection.Forward
                        }
                        onDoubleTap(direction)
                    },
                )
            }
            .pointerInput(enabled, onTransform) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.isEmpty()) break
                        if (pressedChanges.size < 2) continue

                        val first = pressedChanges[0]
                        val second = pressedChanges[1]
                        val previousCentroid =
                            centroidOf(first.previousPosition, second.previousPosition)
                        val centroid = centroidOf(first.position, second.position)
                        val previousDistance =
                            first.previousPosition.distanceTo(second.previousPosition)
                        val currentDistance = first.position.distanceTo(second.position)
                        val zoomChange =
                            if (previousDistance > 0f) currentDistance / previousDistance else 1f
                        val pan = centroid - previousCentroid
                        if (zoomChange == 1f && pan == Offset.Zero) continue

                        onTransform(centroid, pan, zoomChange)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    )
}

private fun centroidOf(first: Offset, second: Offset): Offset =
    Offset(
        x = (first.x + second.x) / 2f,
        y = (first.y + second.y) / 2f,
    )

private fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}
