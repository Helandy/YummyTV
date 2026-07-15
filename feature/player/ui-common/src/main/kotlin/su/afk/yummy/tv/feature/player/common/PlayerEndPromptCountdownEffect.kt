package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/** Секундный отсчёт промпта следующего эпизода; по нулю вызывает [onFinished]. */
@Composable
fun PlayerEndPromptCountdownEffect(
    promptState: PlayerEndPromptState,
    contentKey: String,
    onPromptStateChange: (PlayerEndPromptState) -> Unit,
    onFinished: () -> Unit,
) {
    LaunchedEffect(promptState, contentKey) {
        val countdown = promptState as? PlayerEndPromptState.WithCountdown
            ?: return@LaunchedEffect
        if (countdown.seconds <= 0) {
            withFrameNanos { }
            onFinished()
        } else {
            delay(1.seconds)
            onPromptStateChange(
                PlayerEndPromptState.WithCountdown(countdown.seconds - 1)
            )
        }
    }
}
