package su.afk.yummy.tv.domain.player.model

data class PlayerSourceBalancer(
    val name: String,
    val dubbings: List<PlayerSourceDubbing> = emptyList(),
)
