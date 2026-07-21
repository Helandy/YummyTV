package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Composable
internal fun TvPlayerHiddenKeyOverlay(
    focusRequester: FocusRequester,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Back -> false

                    Key.DirectionLeft -> {
                        onSeekBackward()
                        true
                    }

                    Key.DirectionRight -> {
                        onSeekForward()
                        true
                    }

                    Key.DirectionUp,
                    Key.DirectionDown -> {
                        onInteraction()
                        true
                    }

                    else -> {
                        onInteraction()
                        true
                    }
                }
            }
            .focusable(),
    )
}
