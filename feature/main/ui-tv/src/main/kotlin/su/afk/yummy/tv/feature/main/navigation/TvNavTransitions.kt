package su.afk.yummy.tv.feature.main.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import su.afk.yummy.tv.feature.player.navigator.PLAYER_CONTENT_KEY

/** Переход вперёд: новый экран проявляется, слегка уменьшаясь до своего размера. */
internal val tvNavTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) +
        scaleIn(
            initialScale = TV_NAV_TRANSITION_SCALE,
            animationSpec = tween(TV_NAV_TRANSITION_MILLIS),
        ) togetherWith fadeOut(tween(TV_NAV_TRANSITION_MILLIS))
}

/**
 * Переход назад: уходящий экран гаснет, увеличиваясь.
 *
 * Из плеера выходим без анимации: SurfaceView не подчиняется alpha/scale Compose, и последний
 * кадр видео висел бы поверх возвращаемого экрана до конца перехода.
 */
internal val tvNavPopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (initialState.entries.any { it.contentKey == PLAYER_CONTENT_KEY }) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) togetherWith
            fadeOut(tween(TV_NAV_TRANSITION_MILLIS)) +
            scaleOut(
                targetScale = TV_NAV_TRANSITION_SCALE,
                animationSpec = tween(TV_NAV_TRANSITION_MILLIS),
            )
    }
}

private const val TV_NAV_TRANSITION_MILLIS = 280
private const val TV_NAV_TRANSITION_SCALE = 1.05f
