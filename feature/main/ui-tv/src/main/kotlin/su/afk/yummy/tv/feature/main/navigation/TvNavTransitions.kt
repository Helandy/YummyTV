package su.afk.yummy.tv.feature.main.navigation

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
import su.afk.yummy.tv.feature.player.navigator.PLAYER_CONTENT_KEY

private typealias NavTransitionSpec = AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform

private val noTransition: ContentTransform = EnterTransition.None togetherWith ExitTransition.None

/**
 * Переход вперёд в выбранном [style]. На ТВ слайд мягче, чем на мобилке: новый экран смещается
 * лишь на долю ширины, чтобы движение не рябило на большом экране.
 */
internal fun tvNavTransitionSpec(style: NavTransitionStyle): NavTransitionSpec = when (style) {
    NavTransitionStyle.SLIDE -> {
        {
            slideInHorizontally(tween(TV_NAV_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) {
                it / TV_NAV_SLIDE_DIVIDER
            } + fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) togetherWith
                slideOutHorizontally(tween(TV_NAV_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) {
                    -it / TV_NAV_SLIDE_DIVIDER
                } + fadeOut(tween(TV_NAV_TRANSITION_MILLIS))
        }
    }

    NavTransitionStyle.FADE_SCALE -> {
        {
            fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) +
                scaleIn(
                    initialScale = TV_NAV_TRANSITION_SCALE,
                    animationSpec = tween(TV_NAV_TRANSITION_MILLIS),
                ) togetherWith fadeOut(tween(TV_NAV_TRANSITION_MILLIS))
        }
    }

    NavTransitionStyle.NONE -> {
        { noTransition }
    }
}

/**
 * Переход назад в выбранном [style].
 *
 * Из плеера выходим без анимации: SurfaceView не подчиняется alpha/scale Compose, и последний
 * кадр видео висел бы поверх возвращаемого экрана до конца перехода.
 */
internal fun tvNavPopTransitionSpec(style: NavTransitionStyle): NavTransitionSpec = when (style) {
    NavTransitionStyle.SLIDE -> {
        {
            if (initialState.entries.any { it.contentKey == PLAYER_CONTENT_KEY }) {
                noTransition
            } else {
                slideInHorizontally(tween(TV_NAV_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) {
                    -it / TV_NAV_SLIDE_DIVIDER
                } + fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) togetherWith
                    slideOutHorizontally(tween(TV_NAV_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) {
                        it / TV_NAV_SLIDE_DIVIDER
                    } + fadeOut(tween(TV_NAV_TRANSITION_MILLIS))
            }
        }
    }

    NavTransitionStyle.FADE_SCALE -> {
        {
            if (initialState.entries.any { it.contentKey == PLAYER_CONTENT_KEY }) {
                noTransition
            } else {
                fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) togetherWith
                    fadeOut(tween(TV_NAV_TRANSITION_MILLIS)) +
                    scaleOut(
                        targetScale = TV_NAV_TRANSITION_SCALE,
                        animationSpec = tween(TV_NAV_TRANSITION_MILLIS),
                    )
            }
        }
    }

    NavTransitionStyle.NONE -> {
        { noTransition }
    }
}

private const val TV_NAV_TRANSITION_MILLIS = 280
private const val TV_NAV_TRANSITION_SCALE = 1.05f
private const val TV_NAV_SLIDE_DIVIDER = 10
