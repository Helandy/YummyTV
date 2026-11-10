package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
 * Запускает тур [tourId] на экране, если он ещё не пройден. Небольшая задержка нужна, чтобы
 * помеченные элементы успели отрисоваться и сообщить свои границы.
 */
@Composable
fun OnboardingTourEffect(tourId: String, steps: List<OnboardingStep>, ready: Boolean = true) {
    val controller = LocalOnboardingController.current ?: return
    val pending = ready && controller.isPending(tourId)
    LaunchedEffect(controller, tourId, pending) {
        if (!pending) return@LaunchedEffect
        delay(START_DELAY_MS)
        controller.start(tourId, steps)
    }
}

private const val START_DELAY_MS = 700L
