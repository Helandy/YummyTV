package su.afk.yummy.tv.core.network.dns

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

/** CDN картинок TMDB: у части провайдеров его DNS-имена подменены на `127.0.0.1`. */
private val TMDB_IMAGE_HOSTS = setOf("image.tmdb.org", "media.themoviedb.org")

/**
 * DNS общего OkHttp-клиента: хосты картинок TMDB при блокировке резолвятся через Quad9 (DNS-over-HTTPS, адрес
 * `9.9.9.9` задан сразу, поэтому для самого DoH обычный DNS не нужен), остальные — системным DNS.
 */
fun tmdbImageDns(): Dns {
    // Клиент и DoH создаются лениво: на старте приложения они не нужны, а без блокировки не понадобятся вовсе.
    val quad9 by lazy {
        DnsOverHttps.Builder()
            .client(OkHttpClient())
            .url("https://dns.quad9.net/dns-query".toHttpUrl())
            .bootstrapDnsHosts(InetAddress.getByName("9.9.9.9"), InetAddress.getByName("149.112.112.112"))
            .build()
    }
    return BlockedHostDns(hosts = TMDB_IMAGE_HOSTS, fallback = Dns { hostname -> quad9.lookup(hostname) })
}
