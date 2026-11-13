package su.afk.yummy.tv.core.designsystem.motion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Единые длительности и кривые UI-переходов. Самостоятельные циклы (шиммер, сезонные декорации,
 * таймеры плеера) сюда не относятся и держат свои константы рядом с собой.
 */
object YummyMotion {
    /** Исчезновение и мелкий отклик: уходящий контент, сворачивание. */
    const val Quick = 120

    /** Состояние элемента: фокус, выбор чипа, появление небольшого блока. */
    const val Short = 180

    /** Переход между экранами и панелями. */
    const val Medium = 280

    /** Крупное появление: hero, фоновые изображения. */
    const val Long = 380

    val Standard: Easing = FastOutSlowInEasing

    /** Пружина масштаба при фокусе и нажатии: без отскока, чтобы карточка не «дрожала». */
    fun <T> scaleSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}
