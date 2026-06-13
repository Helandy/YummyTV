package su.afk.yummy.tv.feature.player.common

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

enum class StepSeekDirection(val sign: Int) {
    Backward(-1),
    Forward(1),
}

fun Long.formatSignedSeconds(): String {
    val seconds = this / 1_000L
    val prefix = if (seconds > 0) "+" else ""
    return "${prefix}${seconds}s"
}

const val STEP_SEEK_RESET_MS = 1_500L
val STEP_SEEK_OFFSETS_MS = longArrayOf(5_000L, 10_000L, 15_000L)
