package su.afk.yummy.tv.feature.main.mobile.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import su.afk.yummy.tv.core.model.settings.NavTransitionStyle

private typealias NavTransitionSpec = AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform

/** Переход вперёд в выбранном [style]. */
internal fun mobileNavTransitionSpec(style: NavTransitionStyle): NavTransitionSpec = when (style) {
    NavTransitionStyle.SLIDE -> {
        {
            slideInHorizontally(tween(MOBILE_NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(MOBILE_NAV_SLIDE_MILLIS)) togetherWith
                slideOutHorizontally(tween(MOBILE_NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)) {
                    -it / MOBILE_NAV_PARALLAX_DIVIDER
                } + fadeOut(tween(MOBILE_NAV_SLIDE_MILLIS))
        }
    }

    NavTransitionStyle.FADE_SCALE -> {
        {
            fadeIn(
                tween(
                    durationMillis = MOBILE_NAV_FADE_IN_MILLIS,
                    delayMillis = MOBILE_NAV_FADE_IN_DELAY_MILLIS,
                ),
            ) + scaleIn(
                initialScale = MOBILE_NAV_TRANSITION_SCALE,
                animationSpec = tween(MOBILE_NAV_TRANSITION_MILLIS),
            ) togetherWith fadeOut(tween(MOBILE_NAV_FADE_OUT_MILLIS))
        }
    }

    NavTransitionStyle.NONE -> {
        { EnterTransition.None togetherWith ExitTransition.None }
    }
}

/** Переход назад в выбранном [style]: зеркален переходу вперёд, уходящий экран остаётся сверху. */
internal fun mobileNavPopTransitionSpec(style: NavTransitionStyle): NavTransitionSpec = when (style) {
    NavTransitionStyle.SLIDE -> {
        {
            (
                slideInHorizontally(tween(MOBILE_NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)) {
                    -it / MOBILE_NAV_PARALLAX_DIVIDER
                } + fadeIn(tween(MOBILE_NAV_SLIDE_MILLIS)) togetherWith
                    slideOutHorizontally(tween(MOBILE_NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)) { it } +
                    fadeOut(tween(MOBILE_NAV_SLIDE_MILLIS))
                ).apply { targetContentZIndex = -1f }
        }
    }

    NavTransitionStyle.FADE_SCALE -> {
        {
            fadeIn(
                tween(
                    durationMillis = MOBILE_NAV_FADE_IN_MILLIS,
                    delayMillis = MOBILE_NAV_FADE_IN_DELAY_MILLIS,
                ),
            ) togetherWith fadeOut(tween(MOBILE_NAV_FADE_OUT_MILLIS)) +
                scaleOut(
                    targetScale = MOBILE_NAV_TRANSITION_SCALE,
                    animationSpec = tween(MOBILE_NAV_TRANSITION_MILLIS),
                )
        }
    }

    NavTransitionStyle.NONE -> {
        { EnterTransition.None togetherWith ExitTransition.None }
    }
}

private const val MOBILE_NAV_TRANSITION_MILLIS = 300
private const val MOBILE_NAV_SLIDE_MILLIS = 320
private const val MOBILE_NAV_PARALLAX_DIVIDER = 5
private const val MOBILE_NAV_FADE_IN_MILLIS = 220
private const val MOBILE_NAV_FADE_IN_DELAY_MILLIS = 60
private const val MOBILE_NAV_FADE_OUT_MILLIS = 120
private const val MOBILE_NAV_TRANSITION_SCALE = 0.94f
