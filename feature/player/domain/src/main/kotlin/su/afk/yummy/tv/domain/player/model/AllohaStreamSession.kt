package su.afk.yummy.tv.domain.player.model

/** A live Alloha browser session whose signed headers can rotate while it is in use. */
interface AllohaStreamSession : AutoCloseable {
    val id: String
    val sourceKey: String
    val initialStream: PlayerStreamResolveResult.Stream
    val playbackUrl: String
    val qualityUrls: LinkedHashMap<String, String>
    fun currentHeaders(): Map<String, String>
    fun currentMasterUrl(): String
    fun expiresAtMs(): Long?
    fun refresh()
    fun selectQuality(label: String)
    override fun close()
}
