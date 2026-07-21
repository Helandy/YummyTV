package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/** Переключает контролы по основному клику мыши на свободной области видео. */
@Composable
internal fun TvPlayerPointerOverlay(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!currentEvent.buttons.isPrimaryPressed) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    if (waitForUpOrCancellation() != null) onClick()
                }
            },
    )
}
