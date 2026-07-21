package su.afk.yummy.tv.domain.player.model

data class PlayerSourceGraph(
    val balancers: List<PlayerSourceBalancer> = emptyList(),
    val selection: PlayerSourceSelection = PlayerSourceSelection(),
)
