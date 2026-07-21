package su.afk.yummy.tv.feature.player.mobile.view.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.R

@Composable
internal fun GestureTutorialDetails(
    step: GestureTutorialStep,
    currentStep: Int,
    stepCount: Int,
    accent: Color,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, description) = step.text()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            modifier = Modifier.padding(top = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(stepCount) { index ->
                Box(
                    Modifier
                        .size(if (index == currentStep) 10.dp else 7.dp)
                        .background(
                            color = if (index == currentStep) {
                                accent
                            } else {
                                Color.White.copy(alpha = 0.32f)
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .padding(top = 20.dp)
                .widthIn(min = 180.dp),
        ) {
            Text(
                text = stringResource(
                    if (currentStep == stepCount - 1) {
                        R.string.player_mobile_gesture_tutorial_thanks
                    } else {
                        R.string.player_mobile_gesture_tutorial_next
                    }
                ),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun GestureTutorialStep.text(): Pair<String, String> = when (this) {
    GestureTutorialStep.Tap -> stringResource(
        R.string.player_mobile_gesture_tutorial_tap_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_tap_description)

    GestureTutorialStep.Seek -> stringResource(
        R.string.player_mobile_gesture_tutorial_seek_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_seek_description)

    GestureTutorialStep.Vertical -> stringResource(
        R.string.player_mobile_gesture_tutorial_vertical_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_vertical_description)

    GestureTutorialStep.Zoom -> stringResource(
        R.string.player_mobile_gesture_tutorial_zoom_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_zoom_description)

    GestureTutorialStep.Hold -> stringResource(
        R.string.player_mobile_gesture_tutorial_hold_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_hold_description)
}
