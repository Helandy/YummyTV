package su.afk.yummy.tv.data.player.extractor.alloha

import java.net.URL

/**
 * Picks the working CDN out of the mirrors Alloha lists for one quality (`"url1 or url2"`).
 *
 * The first mirror is the default; it is probed once per host, and when it does not answer the
 * list is reversed, so the last mirror wins. The verdict is cached per host: a node that is down is
 * down for every quality on it, and the probe costs a request.
 *
 * @param isReachable whether the CDN serves [url] with the headers the player will use.
 */
internal class AllohaMirrorPicker(
    private val isReachable: (url: String) -> Boolean,
) {
    private val verdicts = HashMap<String, Boolean>()

    /** The mirror to play from [mirrors], which must not be empty. */
    fun pick(mirrors: List<String>): String {
        val first = mirrors.first()
        if (mirrors.size == 1) return first
        val host = hostOf(first)
        val firstWorks = verdicts.getOrPut(host) { isReachable(first) }
        return if (firstWorks) first else mirrors.last()
    }

    private fun hostOf(url: String): String =
        runCatching { URL(url).host }.getOrNull()?.takeIf(String::isNotBlank) ?: url
}
