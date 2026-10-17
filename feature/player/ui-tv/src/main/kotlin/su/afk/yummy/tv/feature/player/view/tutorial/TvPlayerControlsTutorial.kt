package su.afk.yummy.tv.feature.player.view.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.feature.player.model.TvControlsTutorialStep
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.view.player.TvControlButton

/** Полноэкранное одноразовое обучение управлению ТВ-плеером с пульта. */
@Composable
internal fun TvPlayerControlsTutorial(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = TvControlsTutorialStep.entries
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val accent = MaterialTheme.colorScheme.primary
    val nextFocusRequester = remember { FocusRequester() }
    val illustrationDescription = stringResource(R.string.player_tv_controls_tutorial_illustration)

    BackHandler(enabled = true) {
        // Обучение закрывается только явным подтверждением на последнем шаге.
    }

    LaunchedEffect(Unit) {
        requestFocusUntilTimeout(nextFocusRequester)
    }

    val onNext: () -> Unit = {
        if (stepIndex == steps.lastIndex) {
            onDismiss()
        } else {
            stepIndex += 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        stepIndex = (stepIndex - 1).coerceAtLeast(0)
                        true
                    }

                    Key.DirectionRight -> {
                        stepIndex = (stepIndex + 1).coerceAtMost(steps.lastIndex)
                        true
                    }

                    // Обучение показывает единственную фокусируемую кнопку — гасим Up/Down,
                    // иначе стандартный focus search уводит фокус на панель плеера под оверлеем.
                    Key.DirectionUp, Key.DirectionDown -> true

                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val cardShape = RoundedCornerShape(24.dp)
        Row(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .clip(cardShape)
                .background(YummySemanticColors.PanelScrim)
                .border(1.dp, Color.White.copy(alpha = 0.10f), cardShape)
                .padding(horizontal = 56.dp, vertical = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            TvControlsTutorialIllustration(
                step = step,
                accent = accent,
                modifier = Modifier
                    .width(220.dp)
                    .semantics { contentDescription = illustrationDescription },
            )
            TvControlsTutorialDetails(
                step = step,
                currentStep = stepIndex,
                stepCount = steps.size,
                accent = accent,
                onNext = onNext,
                nextFocusRequester = nextFocusRequester,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TvControlsTutorialDetails(
    step: TvControlsTutorialStep,
    currentStep: Int,
    stepCount: Int,
    accent: Color,
    onNext: () -> Unit,
    nextFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val (title, description) = step.text()
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.player_tv_controls_tutorial_heading),
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            modifier = Modifier.padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text(
                text = stringResource(
                    R.string.player_tv_controls_tutorial_step,
                    currentStep + 1,
                    stepCount,
                ),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        TvControlButton(
            onClick = onNext,
            focusRequester = nextFocusRequester,
            primary = true,
            modifier = Modifier
                .padding(top = 24.dp)
                .widthIn(min = 180.dp),
        ) { color ->
            Text(
                text = stringResource(
                    if (currentStep == stepCount - 1) {
                        R.string.player_tv_controls_tutorial_got_it
                    } else {
                        R.string.player_tv_controls_tutorial_next
                    }
                ),
                color = color,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
