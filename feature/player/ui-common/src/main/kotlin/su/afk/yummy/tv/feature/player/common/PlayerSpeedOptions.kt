package su.afk.yummy.tv.feature.player.common

import kotlin.math.abs

/** Доступные скорости воспроизведения — общий набор для ползунка на мобилке и списка на ТВ. */
object PlayerSpeedOptions {
    val values: List<Float> = listOf(0.5f, 0.75f, 0.9f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

    /** Индекс ближайшей скорости: сохранённое значение могло прийти не из набора (старый шаг 0.1x). */
    fun nearestIndex(speed: Float): Int =
        values.indices.minBy { abs(values[it] - speed) }
}
