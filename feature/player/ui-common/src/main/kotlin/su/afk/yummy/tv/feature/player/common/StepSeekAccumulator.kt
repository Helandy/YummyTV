package su.afk.yummy.tv.feature.player.common

import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.utils.STEP_SEEK_OFFSETS_MS
import su.afk.yummy.tv.feature.player.common.utils.STEP_SEEK_RESET_MS

class StepSeekAccumulator(
    private val offsetsMs: LongArray = STEP_SEEK_OFFSETS_MS,
    private val resetMs: Long = STEP_SEEK_RESET_MS,
) {
    private var lastSeekTimeMs = 0L
    private var stepCount = 0
    private var lastDirection: StepSeekDirection? = null

    var totalOffsetMs = 0L
        private set

    fun next(direction: StepSeekDirection, nowMs: Long): Long {
        if (nowMs - lastSeekTimeMs > resetMs || lastDirection != direction) {
            stepCount = 0
            totalOffsetMs = 0L
        }
        stepCount = (stepCount + 1).coerceAtMost(offsetsMs.size)
        lastSeekTimeMs = nowMs
        lastDirection = direction

        val offsetMs = offsetsMs[stepCount - 1] * direction.sign
        totalOffsetMs += offsetMs
        return offsetMs
    }
}
