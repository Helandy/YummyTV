package su.afk.yummy.tv.feature.player.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.presentation.R

internal enum class SeekDirection(val sign: Int) {
    Backward(-1),
    Forward(1),
}

internal enum class PanelReturnFocusTarget {
    Quality,
    Dubbing,
    Balancer,
    Speed,
}

internal data class ActiveSkip(
    val key: String,
    val type: ActiveSkipType,
    val segment: PlayerSkipSegment,
)

internal enum class ActiveSkipType(@param:StringRes val skippedMessageRes: Int) {
    Opening(R.string.player_opening_skipped),
    Ending(R.string.player_ending_skipped),
}
