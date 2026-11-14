package su.afk.yummy.tv.feature.player.mobile.utils

import java.util.Locale
import kotlin.math.roundToInt

internal const val MOBILE_PLAYER_SPEED_MIN = 0.25f
internal const val MOBILE_PLAYER_SPEED_MAX = 3f
private const val MOBILE_PLAYER_SPEED_STEP = 0.05f

/** Быстрые значения скорости, как в плеере YouTube. */
internal val MobilePlayerSpeedChips: List<Float> =
    listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

/** Скорость в пределах диапазона и с шагом 0.05: float-хвосты ползунка не попадают в настройки. */
internal fun Float.toMobilePlayerSpeed(): Float =
    ((this.coerceIn(MOBILE_PLAYER_SPEED_MIN, MOBILE_PLAYER_SPEED_MAX) / MOBILE_PLAYER_SPEED_STEP).roundToInt() *
        MOBILE_PLAYER_SPEED_STEP).let { (it * 100f).roundToInt() / 100f }

/** «1,00x» для заголовка страницы скорости. */
internal fun Float.toSpeedTitle(): String = String.format(Locale.getDefault(), "%.2fx", this)

/** «1,5» / «1,0» для чипа. */
internal fun Float.toSpeedChipLabel(): String = String.format(Locale.getDefault(), "%.2f", this)
    .trimEnd('0').let { if (it.endsWith(',') || it.endsWith('.')) it + "0" else it }

/** Для строки меню: «1x», «1,5x». */
internal fun Float.toSpeedMenuValue(): String = toSpeedChipLabel().let { "${it}x" }

internal const val MOBILE_PLAYER_SPEED_SLIDER_STEPS = 54
