package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Единственный элемент, доступный при заблокированном управлении. Снимается только удержанием,
 * чтобы случайный тап/свайп не разблокировал плеер; короткий тап показывает подсказку.
 */
@Composable
internal fun MobilePlayerUnlockButton(
    visible: Boolean,
    onUnlock: () -> Unit,
    /** Любое касание кнопки: родитель продлевает время её показа. */
    onTouched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val holdProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var hintVisible by remember { mutableStateOf(false) }
    val unlockLabel = stringResource(R.string.player_mobile_unlock)

    LaunchedEffect(hintVisible) {
        if (hintVisible) {
            delay(UNLOCK_HINT_DURATION_MS)
            hintVisible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = unlockLabel
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onTouched()
                                val hold = scope.launch {
                                    holdProgress.snapTo(0f)
                                    holdProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = UNLOCK_HOLD_DURATION_MS,
                                            easing = LinearEasing,
                                        ),
                                    )
                                    onUnlock()
                                }
                                val released = tryAwaitRelease()
                                if (hold.isActive) {
                                    hold.cancel()
                                    scope.launch { holdProgress.snapTo(0f) }
                                    if (released) hintVisible = true
                                }
                            },
                        )
                    },
            ) {
                CircularProgressIndicator(
                    progress = { holdProgress.value },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(56.dp),
                )
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            AnimatedVisibility(visible = hintVisible, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.player_mobile_unlock_hint),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private const val UNLOCK_HOLD_DURATION_MS = 600
private const val UNLOCK_HINT_DURATION_MS = 1_800L
