package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.annotation.StringRes

/** Шаг спотлайт-тура: подсвечивает элемент, помеченный `Modifier.onboardingTarget(targetId)`. */
data class OnboardingStep(
    val targetId: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val textRes: Int,
)
