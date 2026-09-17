package su.afk.yummy.tv.domain.account.model

/**
 * Represents a TV device discovered on the local network.
 */
data class DiscoveredDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
)
