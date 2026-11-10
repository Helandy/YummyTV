package su.afk.yummy.tv.feature.details.mobile.details.utils

import su.afk.yummy.tv.core.designsystem.onboarding.OnboardingStep
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.feature.details.presentation.R

/** Цель онбординга: основная кнопка «Смотреть». */
internal const val MOBILE_DETAILS_WATCH_TARGET = "details_watch"

/** Цель онбординга: карточка действия [action] под кнопкой «Смотреть». */
internal fun mobileDetailsActionTargetId(action: DetailsButtonAction): String = "details_action_$action"

internal val mobileDetailsTourSteps: List<OnboardingStep> = listOf(
    OnboardingStep(MOBILE_DETAILS_WATCH_TARGET, R.string.details_onboarding_watch_title, R.string.details_onboarding_watch_text),
    OnboardingStep(
        mobileDetailsActionTargetId(DetailsButtonAction.EPISODES),
        R.string.details_onboarding_episodes_title,
        R.string.details_onboarding_episodes_text,
    ),
    OnboardingStep(
        mobileDetailsActionTargetId(DetailsButtonAction.SUBSCRIPTIONS),
        R.string.details_onboarding_subscriptions_title,
        R.string.details_onboarding_subscriptions_text,
    ),
)
