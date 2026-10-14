package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val TvLazyGridFocusRestoreTimeout = 500.milliseconds

/**
 * Retries [FocusRequester.requestFocus] on every frame until it succeeds or [timeout] elapses,
 * instead of a fixed number of attempts - the item may not be focusable yet on the frame right
 * after it's scrolled into view, and a hardcoded retry count is either too short (still racy) or
 * wastefully long (burns frames after focus would've already succeeded). Shared beyond this file's
 * own grid/list restore use: any TV screen that needs to grab focus on something not yet guaranteed
 * to be attached (e.g. right after a scroll, or right after a conditionally-shown overlay appears)
 * should reuse this instead of rolling its own `repeat(N) { requestFocus(); withFrameNanos {} }` or
 * a hand-written `withTimeoutOrNull { while (!focused) { ... } }` copy.
 */
suspend fun requestFocusUntilTimeout(
    requester: FocusRequester,
    timeout: Duration = TvLazyGridFocusRestoreTimeout,
): Boolean = requestFocusUntilTimeout(timeout) { requester.requestFocus() }

/**
 * Same as the [FocusRequester] overload, but for targets that are not a single requester - e.g. a
 * row that picks its own child to focus, or a control resolved by a focus-target enum. [attempt]
 * returns whether focus was taken; it is tried once per frame until [timeout].
 *
 * The first attempt deliberately waits one frame: callers launched from a LaunchedEffect rely on
 * landing after focus moves made in the same frame (e.g. the main scaffold handing focus to the
 * screen's preferred requester) - trying synchronously lets those moves override the result.
 */
suspend fun requestFocusUntilTimeout(
    timeout: Duration = TvLazyGridFocusRestoreTimeout,
    attempt: suspend () -> Boolean,
): Boolean =
    withTimeoutOrNull(timeout) {
        do {
            withFrameNanos { }
        } while (!tryAttempt(attempt))
        true
    } ?: false

// requestFocus() throws while the target is not attached yet - treat it as "not this frame".
private suspend fun tryAttempt(attempt: suspend () -> Boolean): Boolean =
    try {
        attempt()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }
