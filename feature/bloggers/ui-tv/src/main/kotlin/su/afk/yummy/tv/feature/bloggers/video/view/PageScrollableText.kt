package su.afk.yummy.tv.feature.bloggers.video.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch

/**
 * Текст описания, который при фокусе листает весь экран по D-pad: вверх/вниз крутят [scrollState],
 * пока есть куда, иначе фокус уходит дальше (без залипания).
 */
@Composable
internal fun PageScrollableText(
    text: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val step = 240f
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> if (scrollState.canScrollForward) {
                        scope.launch { scrollState.animateScrollBy(step) }
                        true
                    } else false

                    Key.DirectionUp -> if (scrollState.canScrollBackward) {
                        scope.launch { scrollState.animateScrollBy(-step) }
                        true
                    } else false

                    else -> false
                }
            }
            .focusable(),
    )
}

