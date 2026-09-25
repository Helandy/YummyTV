package su.afk.yummy.tv.feature.main.mobile.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

/** Переход вперёд: новый экран проявляется с лёгким увеличением, старый гаснет. */
internal val mobileNavTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
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

/** Переход назад: уходящий экран уменьшается и гаснет, предыдущий проявляется. */
internal val mobileNavPopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
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

private const val MOBILE_NAV_TRANSITION_MILLIS = 300
private const val MOBILE_NAV_FADE_IN_MILLIS = 220
private const val MOBILE_NAV_FADE_IN_DELAY_MILLIS = 60
private const val MOBILE_NAV_FADE_OUT_MILLIS = 120
private const val MOBILE_NAV_TRANSITION_SCALE = 0.94f
