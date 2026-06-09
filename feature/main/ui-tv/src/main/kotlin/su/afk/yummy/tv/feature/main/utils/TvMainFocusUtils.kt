package su.afk.yummy.tv.feature.main.utils

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.navigation.root.RootTab

private const val ContentFocusRestoreTimeoutMillis = 900L

internal fun Modifier.moveFocusToContentOnKey(
    onMoveToContent: (force: Boolean) -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionRight -> {
                onMoveToContent(true)
                true
            }

            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter -> {
                onMoveToContent(false)
                true
            }

            else -> false
        }
    }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            when (event.key) {
                Key.DirectionRight -> {
                    onMoveToContent(true)
                    true
                }

                Key.DirectionCenter,
                Key.Enter,
                Key.NumPadEnter -> {
                    onMoveToContent(false)
                    true
                }

                else -> false
            }
        }

internal suspend fun requestFocusOnFrameBoundary(
    requester: FocusRequester,
): Boolean =
    withTimeoutOrNull<Boolean>(ContentFocusRestoreTimeoutMillis) {
        repeat(2) { withFrameNanos { } }
        var focused = false
        while (!focused) {
            focused = runCatching { requester.requestFocus() }.getOrDefault(false)
            withFrameNanos { }
        }
        focused
    } ?: false

internal fun Any?.isContentFocusKeyFor(root: RootTab): Boolean =
    this == null || (this is Pair<*, *> && first == root)
