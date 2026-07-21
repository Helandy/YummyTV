package su.afk.yummy.tv.feature.player.mobile.view.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.R

@Composable
internal fun MobilePlayerGestureTutorial(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = GestureTutorialStep.entries
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val accent = MaterialTheme.colorScheme.primary
    val illustrationDescription =
        stringResource(R.string.player_mobile_gesture_tutorial_illustration)

    BackHandler(enabled = true) {
        // Обучение закрывается только явным подтверждением на последнем шаге.
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
    ) {
        TutorialTouchBlocker()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            val isLandscape = maxWidth > maxHeight
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.player_mobile_gesture_tutorial_heading),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        R.string.player_mobile_gesture_tutorial_step,
                        stepIndex + 1,
                        steps.size,
                    ),
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )

                val onNext: () -> Unit = {
                    if (stepIndex == steps.lastIndex) {
                        onDismiss()
                    } else {
                        stepIndex += 1
                    }
                }
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        GestureTutorialIllustration(
                            step = step,
                            accent = accent,
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .semantics { contentDescription = illustrationDescription },
                        )
                        GestureTutorialDetails(
                            step = step,
                            currentStep = stepIndex,
                            stepCount = steps.size,
                            accent = accent,
                            onNext = onNext,
                            modifier = Modifier
                                .weight(0.85f)
                                .widthIn(max = 420.dp),
                        )
                    }
                } else {
                    GestureTutorialIllustration(
                        step = step,
                        accent = accent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .semantics { contentDescription = illustrationDescription },
                    )
                    GestureTutorialDetails(
                        step = step,
                        currentStep = stepIndex,
                        stepCount = steps.size,
                        accent = accent,
                        onNext = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialTouchBlocker() {
    // Нижний слой поглощает касания вне кнопки и не отдаёт их плееру.
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
    )
}
