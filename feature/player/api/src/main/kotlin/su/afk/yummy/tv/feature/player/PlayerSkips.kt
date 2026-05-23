package su.afk.yummy.tv.feature.player

import kotlinx.serialization.Serializable

@Serializable
data class PlayerSkips(
    val opening: PlayerSkipSegment? = null,
    val ending: PlayerSkipSegment? = null,
) {
    companion object {
        val Empty = PlayerSkips()
    }
}

@Serializable
data class PlayerSkipSegment(
    val startMs: Long,
    val endMs: Long,
)
