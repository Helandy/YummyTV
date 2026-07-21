package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import su.afk.yummy.tv.feature.player.common.StepSeekDirection
import su.afk.yummy.tv.feature.player.mobile.model.MobileVerticalGestureZone
import kotlin.math.sqrt

private const val VERTICAL_GESTURE_EDGE_FRACTION = 0.2f

@Composable
internal fun MobilePlayerGestureLayer(
    enabled: Boolean,
    onTap: () -> Unit,
    onDoubleTap: (StepSeekDirection) -> Unit,
    onTransformStart: () -> Unit,
    onTransform: (centroid: Offset, pan: Offset, zoomChange: Float) -> Unit,
    onTransformEnd: () -> Unit,
    onVerticalDragStart: (zone: MobileVerticalGestureZone) -> Unit,
    onVerticalDrag: (zone: MobileVerticalGestureZone, deltaFraction: Float) -> Unit,
    onVerticalDragEnd: (zone: MobileVerticalGestureZone) -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnTransformStart by rememberUpdatedState(onTransformStart)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnTransformEnd by rememberUpdatedState(onTransformEnd)
    val currentOnVerticalDragStart by rememberUpdatedState(onVerticalDragStart)
    val currentOnVerticalDrag by rememberUpdatedState(onVerticalDrag)
    val currentOnVerticalDragEnd by rememberUpdatedState(onVerticalDragEnd)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    var transforming = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.isEmpty()) {
                            if (transforming) currentOnTransformEnd()
                            break
                        }
                        if (pressedChanges.size < 2) {
                            if (transforming) {
                                transforming = false
                                currentOnTransformEnd()
                            }
                            continue
                        }
                        if (!transforming) {
                            transforming = true
                            currentOnTransformStart()
                        }

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

                        currentOnTransform(centroid, pan, zoomChange)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = { offset ->
                        val direction = if (offset.x < size.width / 2f) {
                            StepSeekDirection.Backward
                        } else {
                            StepSeekDirection.Forward
                        }
                        currentOnDoubleTap(direction)
                    },
                    onLongPress = { currentOnLongPressStart() },
                    onPress = {
                        try {
                            tryAwaitRelease()
                        } finally {
                            currentOnLongPressEnd()
                        }
                    },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureHeight = size.height
                    if (!down.position.isInVerticalGestureArea(gestureHeight)) {
                        return@awaitEachGesture
                    }

                    val zone = if (down.position.x < size.width / 2f) {
                        MobileVerticalGestureZone.Brightness
                    } else {
                        MobileVerticalGestureZone.Volume
                    }
                    var overSlop = 0f
                    val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()
                        overSlop = over
                    }
                    if (drag != null) {
                        currentOnVerticalDragStart(zone)
                        currentOnVerticalDrag(zone, -overSlop / gestureHeight)
                        verticalDrag(drag.id) { change ->
                            currentOnVerticalDrag(
                                zone,
                                -change.positionChange().y / gestureHeight,
                            )
                            change.consume()
                        }
                        currentOnVerticalDragEnd(zone)
                    }
                }
            },
    )
}

private fun Offset.isInVerticalGestureArea(height: Int): Boolean {
    if (height <= 0) return false
    val edgeHeight = height * VERTICAL_GESTURE_EDGE_FRACTION
    return y in edgeHeight..(height - edgeHeight)
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
