package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.R

/**
 * Спотлайт поверх всего экрана: затемнение с вырезом вокруг цели текущего шага и карточка с
 * пояснением. Размещается в корневом `Box`, занимающем всё окно. Шаг, цель которого так и не
 * появилась на экране, молча пропускается.
 */
@Composable
fun OnboardingSpotlightOverlay(
    controller: OnboardingController,
    modifier: Modifier = Modifier,
) {
    val tour = controller.activeTour ?: return
    val step = tour.step

    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Цель за пределами окна (например, внизу не прокрученного экрана) считаем отсутствующей.
    val windowRect = Rect(overlayOrigin, Size(overlaySize.width.toFloat(), overlaySize.height.toFloat()))
    val target = controller.boundsOf(step.targetId)
        ?.takeIf { overlaySize != IntSize.Zero && it.overlaps(windowRect) }
    val currentTarget by rememberUpdatedState(target)

    // Цель могла не отрисоваться (другая раскладка, пустой список): не зависаем на пустом шаге.
    LaunchedEffect(tour.id, tour.stepIndex) {
        delay(MISSING_TARGET_TIMEOUT_MS)
        if (currentTarget == null) controller.next()
    }
    BackHandler(onBack = controller::skip)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                overlayOrigin = it.positionInRoot()
                overlaySize = it.size
            }
    ) {
        if (target == null) return@Box
        val padPx = with(density) { SPOTLIGHT_PADDING.toPx() }
        val radiusPx = with(density) { SPOTLIGHT_RADIUS.toPx() }
        val local = target.translate(-overlayOrigin)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = controller::next,
                ),
        ) {
            drawRect(Color.Black.copy(alpha = 0.78f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(local.left - padPx, local.top - padPx),
                size = Size(local.width + padPx * 2, local.height + padPx * 2),
                cornerRadius = CornerRadius(radiusPx),
                blendMode = BlendMode.Clear,
            )
        }

        val targetInTopHalf = local.center.y < overlaySize.height / 2f
        val cardOffsetPx = with(density) { CARD_GAP.toPx() }
        val cardModifier = if (targetInTopHalf) {
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(density) { (local.bottom + padPx + cardOffsetPx).toDp() })
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = with(density) {
                        (overlaySize.height - local.top + padPx + cardOffsetPx).toDp()
                    },
                )
        }
        Column(
            modifier = cardModifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(step.textRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step_counter, tour.stepIndex + 1, tour.steps.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!tour.isLast) {
                        OnboardingButton(
                            text = stringResource(R.string.onboarding_skip),
                            accent = false,
                            onClick = controller::skip,
                        )
                    }
                    OnboardingButton(
                        text = stringResource(if (tour.isLast) R.string.onboarding_done else R.string.onboarding_next),
                        accent = true,
                        onClick = controller::next,
                    )
                }
            }
        }
    }
}

private val SPOTLIGHT_PADDING = 6.dp
private val SPOTLIGHT_RADIUS = 14.dp
private val CARD_GAP = 12.dp
private const val MISSING_TARGET_TIMEOUT_MS = 1_200L
