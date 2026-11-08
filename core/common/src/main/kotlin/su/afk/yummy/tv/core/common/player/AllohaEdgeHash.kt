// Part of YummyTV — https://github.com/Helandy/yummy-tv (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.core.common.player

/**
 * The newest `edge_hash` Alloha's player WebSocket handed out. The CDN wants it in the
 * `Accepts-Controls` header and rotates it every few minutes, refusing a stale one with
 * 403 `session_blocked`, so the media requests read it per request instead of from the header
 * map frozen when the stream was resolved. One playback at a time uses it.
 */
object AllohaEdgeHash {
    const val HEADER = "accepts-controls"

    @Volatile
    var current: String? = null

    /** Bytes of media the player has loaded from the CDN in this playback; reported to Alloha as `bytesLoadedTotal`. */
    val bytesLoaded = java.util.concurrent.atomic.AtomicLong(0L)
}
