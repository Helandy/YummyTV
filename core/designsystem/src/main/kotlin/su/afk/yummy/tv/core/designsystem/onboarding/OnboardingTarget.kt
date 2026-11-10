package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.delay

/** Помечает элемент как цель шага онбординга [targetId]; вне главного шелла ничего не делает. */
@Composable
fun Modifier.onboardingTarget(targetId: String): Modifier {
    val controller = LocalOnboardingController.current ?: return this
    DisposableEffect(controller, targetId) {
        onDispose { controller.unregisterTarget(targetId) }
    }
    return this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) controller.registerTarget(targetId, coordinates.boundsInRoot())
    }
}

/**
 * Показывает тур [tourId], пока [show] — решение ViewModel экрана: тур не пройден и экран готов.
 * Небольшая задержка нужна, чтобы помеченные элементы успели отрисоваться и сообщить границы.
 * Если сейчас идёт другой тур, ждёт его конца. [onFinished] вызывается при завершении и пропуске;
 * ViewModel после него должна погасить [show].
 */
@Composable
fun OnboardingTourEffect(
    tourId: String,
    steps: List<OnboardingStep>,
    show: Boolean,
    onFinished: () -> Unit,
) {
    val controller = LocalOnboardingController.current ?: return
    val currentOnFinished by rememberUpdatedState(onFinished)
    // Между завершением тура и тем, как ViewModel погасит [show], тур не должен стартовать снова.
    var finished by remember(tourId) { mutableStateOf(false) }
    val canStart = show && !finished && controller.activeTour == null
    LaunchedEffect(show) {
        if (!show) finished = false
    }
    LaunchedEffect(controller, tourId, canStart) {
        if (!canStart) return@LaunchedEffect
        delay(START_DELAY_MS)
        controller.start(tourId, steps) {
            finished = true
            currentOnFinished()
        }
    }
}

private const val START_DELAY_MS = 700L
