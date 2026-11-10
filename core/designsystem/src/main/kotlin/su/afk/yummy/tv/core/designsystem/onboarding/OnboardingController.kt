package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
) {
    val step: OnboardingStep get() = steps[stepIndex]
    val isLast: Boolean get() = stepIndex == steps.lastIndex
}

/**
 * Состояние спотлайт-подсказок: границы помеченных элементов и активный тур.
 * Какие туры уже пройдены, хранится вне контроллера и приходит через [seenTours].
 */
@Stable
class OnboardingController internal constructor() {

    internal var onTourFinished: (tourId: String) -> Unit = {}

    private val targetBounds = mutableStateMapOf<String, Rect>()

    /** Пройденные туры; `null`, пока настройки не загружены (тогда ничего не показываем). */
    var seenTours: Set<String>? by mutableStateOf(null)

    /** `true`, пока поверх идёт другой онбординг (приветственные слайды). */
    var blocked: Boolean by mutableStateOf(false)

    var activeTour: ActiveOnboardingTour? by mutableStateOf(null)
        private set

    fun boundsOf(targetId: String): Rect? = targetBounds[targetId]

    internal fun registerTarget(targetId: String, bounds: Rect) {
        if (targetBounds[targetId] != bounds) targetBounds[targetId] = bounds
    }

    internal fun unregisterTarget(targetId: String) {
        targetBounds.remove(targetId)
    }

    /** Тур [tourId] ещё не пройден и сейчас ничто не мешает его показать. */
    fun isPending(tourId: String): Boolean {
        val seen = seenTours ?: return false
        return tourId !in seen && !blocked && activeTour == null
    }

    fun start(tourId: String, steps: List<OnboardingStep>) {
        if (steps.isEmpty() || !isPending(tourId)) return
        activeTour = ActiveOnboardingTour(tourId, steps, stepIndex = 0)
    }

    fun next() {
        val tour = activeTour ?: return
        if (tour.isLast) finish() else activeTour = ActiveOnboardingTour(tour.id, tour.steps, tour.stepIndex + 1)
    }

    /** Закрывает тур досрочно; он считается пройденным и повторно не покажется. */
    fun skip() = finish()

    private fun finish() {
        val tour = activeTour ?: return
        activeTour = null
        onTourFinished(tour.id)
    }
}

@Composable
fun rememberOnboardingController(
    seenTours: Set<String>?,
    blocked: Boolean,
    onTourFinished: (tourId: String) -> Unit,
): OnboardingController {
    val controller = remember { OnboardingController() }
    SideEffect {
        controller.seenTours = seenTours
        controller.blocked = blocked
        controller.onTourFinished = onTourFinished
    }
    return controller
}
