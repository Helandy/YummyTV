package su.afk.yummy.tv.core.model.settings

import su.afk.yummy.tv.core.model.settings.WatchedThresholds.Companion.MEDIUM_EPISODE_MAX_MS
import su.afk.yummy.tv.core.model.settings.WatchedThresholds.Companion.SHORT_EPISODE_MAX_MS


/**
 * Сколько минут до конца серии может остаться, чтобы она считалась просмотренной.
 * Порог зависит от длины серии: короткие (до [SHORT_EPISODE_MAX_MS]), обычные
 * (до [MEDIUM_EPISODE_MAX_MS]) и длинные.
 */
data class WatchedThresholds(
    val shortMinutes: Int = DEFAULT_SHORT_MINUTES,
    val mediumMinutes: Int = DEFAULT_MEDIUM_MINUTES,
    val longMinutes: Int = DEFAULT_LONG_MINUTES,
) {
    fun coerced(): WatchedThresholds = WatchedThresholds(
        shortMinutes = shortMinutes.coerceIn(SHORT_MINUTES_RANGE),
        mediumMinutes = mediumMinutes.coerceIn(MEDIUM_MINUTES_RANGE),
        longMinutes = longMinutes.coerceIn(LONG_MINUTES_RANGE),
    )

    /** Допустимый остаток до конца для серии длительностью [durationMs]. */
    fun remainingMsFor(durationMs: Long): Long {
        val minutes = when {
            durationMs <= SHORT_EPISODE_MAX_MS -> shortMinutes
            durationMs <= MEDIUM_EPISODE_MAX_MS -> mediumMinutes
            else -> longMinutes
        }
        return minutes * MINUTE_MS
    }

    companion object {
        const val DEFAULT_SHORT_MINUTES = 1
        const val DEFAULT_MEDIUM_MINUTES = 5
        const val DEFAULT_LONG_MINUTES = 10

        val SHORT_MINUTES_RANGE = 1..5
        val MEDIUM_MINUTES_RANGE = 1..15
        val LONG_MINUTES_RANGE = 1..30

        const val SHORT_EPISODE_MAX_MS = 10 * 60 * 1000L
        const val MEDIUM_EPISODE_MAX_MS = 60 * 60 * 1000L

        private const val MINUTE_MS = 60 * 1000L
    }
}
