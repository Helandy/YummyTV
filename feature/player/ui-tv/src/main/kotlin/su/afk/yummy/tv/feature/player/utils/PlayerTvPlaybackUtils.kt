package su.afk.yummy.tv.feature.player.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.PlayerSkipType
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.common.StepSeekDirection
import su.afk.yummy.tv.feature.player.model.ActiveSkip
import su.afk.yummy.tv.feature.player.model.ActiveSkipType
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.SeekDirection
import su.afk.yummy.tv.feature.player.presentation.R
import java.util.Locale

internal fun PanelReturnFocusTarget.toPlayerControlFocusTarget(): PlayerControlFocusTarget =
    when (this) {
        PanelReturnFocusTarget.Quality -> PlayerControlFocusTarget.Quality
        PanelReturnFocusTarget.Dubbing -> PlayerControlFocusTarget.Dubbing
        PanelReturnFocusTarget.Balancer -> PlayerControlFocusTarget.Balancer
        PanelReturnFocusTarget.Resize -> PlayerControlFocusTarget.Resize
        PanelReturnFocusTarget.Speed -> PlayerControlFocusTarget.Speed
    }

internal val SeekDirection.toastIcon: ImageVector
    get() = when (this) {
        SeekDirection.Backward -> Icons.Filled.FastRewind
        SeekDirection.Forward -> Icons.Filled.FastForward
    }

internal fun SeekDirection.toStepSeekDirection(): StepSeekDirection =
    when (this) {
        SeekDirection.Backward -> StepSeekDirection.Backward
        SeekDirection.Forward -> StepSeekDirection.Forward
    }

@Composable
internal fun Int.formatCompactCount(): String = when {
    this >= 1_000_000 -> stringResource(
        R.string.player_count_millions,
        (this / 1_000_000f).formatCompactDecimal(),
    )

    this >= 1_000 -> stringResource(
        R.string.player_count_thousands,
        (this / 1_000f).formatCompactDecimal(),
    )

    else -> toString()
}

private fun Float.formatCompactDecimal(): String =
    if (this % 1f == 0f) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
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
