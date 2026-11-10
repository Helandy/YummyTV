package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/** Контроллер подсказок для экранов; `null` вне главного шелла (превью, тесты) — подсказки тогда не показываются. */
val LocalOnboardingController = compositionLocalOf<OnboardingController?> { null }

/** Тур, который сейчас показывается. */
@Stable
class ActiveOnboardingTour internal constructor(
    val id: String,
    val steps: List<OnboardingStep>,
    val stepIndex: Int,
    internal val onFinished: () -> Unit,
) {
    val step: OnboardingStep get() = steps[stepIndex]
    val isLast: Boolean get() = stepIndex == steps.lastIndex
}

/**
 * Состояние спотлайт-подсказок: границы помеченных элементов и активный тур. Решает ли экран
 * показывать тур и пройден ли он, знает ViewModel экрана; контроллер только рисует тур.
 */
@Stable
class OnboardingController internal constructor() {

    private val targetBounds = mutableStateMapOf<String, Rect>()

    var activeTour: ActiveOnboardingTour? by mutableStateOf(null)
        private set

    fun boundsOf(targetId: String): Rect? = targetBounds[targetId]

    internal fun registerTarget(targetId: String, bounds: Rect) {
        if (targetBounds[targetId] != bounds) targetBounds[targetId] = bounds
    }

    internal fun unregisterTarget(targetId: String) {
        targetBounds.remove(targetId)
    }

    /** Запускает тур, если сейчас не идёт другой; [onFinished] вызывается при завершении и при пропуске. */
    fun start(tourId: String, steps: List<OnboardingStep>, onFinished: () -> Unit) {
        if (steps.isEmpty() || activeTour != null) return
        activeTour = ActiveOnboardingTour(tourId, steps, stepIndex = 0, onFinished = onFinished)
    }

    fun next() {
        val tour = activeTour ?: return
        if (tour.isLast) {
            finish()
        } else {
            activeTour = ActiveOnboardingTour(tour.id, tour.steps, tour.stepIndex + 1, tour.onFinished)
        }
    }

    /** Закрывает тур досрочно; для экрана это то же завершение. */
    fun skip() = finish()

    private fun finish() {
        val tour = activeTour ?: return
        activeTour = null
        tour.onFinished()
    }
}

@Composable
fun rememberOnboardingController(): OnboardingController = remember { OnboardingController() }
