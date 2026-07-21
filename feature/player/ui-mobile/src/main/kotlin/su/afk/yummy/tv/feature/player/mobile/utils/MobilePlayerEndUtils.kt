package su.afk.yummy.tv.feature.player.mobile.utils

internal const val MOBILE_PLAYER_PIP_SEEK_STEP_MS = 10_000L

internal fun isAtMobilePlayerEnd(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0L && durationMs - positionMs <= MOBILE_PLAYER_END_POSITION_TOLERANCE_MS

private const val MOBILE_PLAYER_END_POSITION_TOLERANCE_MS = 500L
