package su.afk.yummy.tv.feature.player.common.utils

/**
 * Возвращает диапазон опенинга как доли `0f..1f` от длительности, или `null`, если опенинга нет
 * либо длительность ещё не известна. Метка на таймлайне рисуется только для валидного отрезка.
 */
fun openingRange(
    startMs: Long?,
    endMs: Long?,
    duration: Long,
): ClosedFloatingPointRange<Float>? {
    if (startMs == null || endMs == null || duration <= 0L || endMs <= startMs) return null
    val start = (startMs.toFloat() / duration).coerceIn(0f, 1f)
    val end = (endMs.toFloat() / duration).coerceIn(0f, 1f)
    return if (end > start) start..end else null
}
