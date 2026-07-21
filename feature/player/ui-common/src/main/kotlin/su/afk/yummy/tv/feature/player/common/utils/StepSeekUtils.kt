package su.afk.yummy.tv.feature.player.common.utils

fun Long.formatSignedSeconds(): String {
    val seconds = this / 1_000L
    val prefix = if (seconds > 0) "+" else ""
    return "${prefix}${seconds}s"
}

const val STEP_SEEK_RESET_MS = 1_500L
val STEP_SEEK_OFFSETS_MS = longArrayOf(5_000L, 10_000L, 15_000L)
