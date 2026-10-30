package su.afk.yummy.tv.core.network.dns

import okhttp3.Dns
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * DNS для хостов, которые провайдеры блокируют подменой ответа (`image.tmdb.org` резолвится в `127.0.0.1`).
 *
 * Сначала спрашивается системный DNS: если он ответил нормально, запасной не трогается, и пользователю без блокировки
 * это ничего не стоит. Ответ с адресом самой машины (loopback или `0.0.0.0`) или ошибка резолва считаются
 * блокировкой, и адрес берётся у [fallback] (DNS-over-HTTPS), результат помнится [CACHE_TTL_MS].
 * Остальные хосты всегда идут в системный DNS.
 */
class BlockedHostDns(
    private val hosts: Set<String>,
    private val fallback: Dns,
    private val system: Dns = Dns.SYSTEM,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : Dns {

    private class Cached(val addresses: List<InetAddress>, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, Cached>()

    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname !in hosts) return system.lookup(hostname)

        val systemResult = runCatching { system.lookup(hostname) }
        systemResult.getOrNull()?.takeIf { addresses -> addresses.isNotEmpty() && addresses.none { it.isBlockedAnswer() } }
            ?.let { return it }

        cache[hostname]?.takeIf { it.expiresAtMs > nowMillis() }?.let { return it.addresses }
        val resolved = fallback.lookup(hostname)
        cache[hostname] = Cached(resolved, nowMillis() + CACHE_TTL_MS)
        return resolved
    }

    private fun InetAddress.isBlockedAnswer(): Boolean = isLoopbackAddress || isAnyLocalAddress

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
