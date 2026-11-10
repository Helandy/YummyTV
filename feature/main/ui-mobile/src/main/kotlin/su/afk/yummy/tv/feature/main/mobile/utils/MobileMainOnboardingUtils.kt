package su.afk.yummy.tv.feature.main.mobile.utils

import su.afk.yummy.tv.core.designsystem.onboarding.OnboardingStep
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.presentation.R

/** Идентификатор цели онбординга для вкладки [destination] (бар и рейка используют один). */
internal fun mobileMainTabTargetId(destination: Any?): String = "main_tab_$destination"

internal val mobileMainTourSteps: List<OnboardingStep> = listOf(
    OnboardingStep(mobileMainTabTargetId(RootTab.CATALOG), R.string.main_onboarding_catalog_title, R.string.main_onboarding_catalog_text),
    OnboardingStep(mobileMainTabTargetId(RootTab.HOME), R.string.main_onboarding_home_title, R.string.main_onboarding_home_text),
    OnboardingStep(mobileMainTabTargetId(RootTab.LIBRARY), R.string.main_onboarding_library_title, R.string.main_onboarding_library_text),
    OnboardingStep(mobileMainTabTargetId(RootTab.ACCOUNT), R.string.main_onboarding_account_title, R.string.main_onboarding_account_text),
)
