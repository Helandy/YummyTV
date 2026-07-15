package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerSkipType
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.model.ActiveSkip
import su.afk.yummy.tv.feature.player.model.ActiveSkipType
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget

internal fun PanelReturnFocusTarget.toPlayerControlFocusTarget(): PlayerControlFocusTarget =
    when (this) {
        PanelReturnFocusTarget.Quality -> PlayerControlFocusTarget.Quality
        PanelReturnFocusTarget.Dubbing -> PlayerControlFocusTarget.Dubbing
        PanelReturnFocusTarget.Balancer -> PlayerControlFocusTarget.Balancer
        PanelReturnFocusTarget.Resize -> PlayerControlFocusTarget.Resize
        PanelReturnFocusTarget.Speed -> PlayerControlFocusTarget.Speed
    }

internal fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

internal fun Float.speedLabel(): String =
    if (this % 1f == 0f) "${toInt()}x" else "${this}x"

internal fun currentSkip(
    skips: PlayerSkips,
    positionMs: Long,
    dismissedKeys: List<String>,
): ActiveSkip? =
    listOfNotNull(
        skips.opening?.let {
            ActiveSkip(
                "opening:${it.startMs}:${it.endMs}",
                ActiveSkipType.Opening,
                it
            )
        },
        skips.ending?.let {
            ActiveSkip(
                "ending:${it.startMs}:${it.endMs}",
                ActiveSkipType.Ending,
                it
            )
        },
    ).firstOrNull { skip ->
        skip.key !in dismissedKeys && positionMs in skip.segment.startMs..skip.segment.endMs
    }

internal fun ActiveSkipType.toPlayerSkipType(): PlayerSkipType =
    when (this) {
        ActiveSkipType.Opening -> PlayerSkipType.Opening
        ActiveSkipType.Ending -> PlayerSkipType.Ending
    }
