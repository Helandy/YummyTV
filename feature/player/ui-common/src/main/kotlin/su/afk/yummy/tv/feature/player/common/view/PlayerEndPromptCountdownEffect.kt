package su.afk.yummy.tv.feature.player.common.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState
import kotlin.time.Duration.Companion.seconds

/** Секундный отсчёт промпта следующего эпизода; по нулю вызывает [onFinished]. */
@Composable
fun PlayerEndPromptCountdownEffect(
    promptState: PlayerEndPromptState,
    contentKey: String,
    onPromptStateChange: (PlayerEndPromptState) -> Unit,
    onFinished: () -> Unit,
) {
    // Отсчёт идёт секундами, а колбэк пересоздаётся при каждой композиции: берём свежий.
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(promptState, contentKey) {
        val countdown = promptState as? PlayerEndPromptState.WithCountdown
            ?: return@LaunchedEffect
        if (countdown.seconds <= 0) {
            withFrameNanos { }
            currentOnFinished()
        } else {
            delay(1.seconds)
            onPromptStateChange(
                PlayerEndPromptState.WithCountdown(countdown.seconds - 1)
            )
        }
    }
}
