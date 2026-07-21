package su.afk.yummy.tv.domain.player.model

data class PlayerSourceSkips(
    val opening: PlayerSourceSkipSegment? = null,
    val ending: PlayerSourceSkipSegment? = null,
) {
    companion object {
        val Empty = PlayerSourceSkips()
    }
}
