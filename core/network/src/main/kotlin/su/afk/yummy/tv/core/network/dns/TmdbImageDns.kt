package su.afk.yummy.tv.core.network.dns

import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/** CDN картинок TMDB: у части провайдеров его DNS-имена подменены на `127.0.0.1`, а узлы бывают медленными или упавшими. */
private val TMDB_IMAGE_HOSTS = setOf("image.tmdb.org", "media.themoviedb.org")

private const val TMDB_CONNECT_TIMEOUT_SECONDS = 3
private const val TMDB_READ_TIMEOUT_SECONDS = 5
private const val DOH_TIMEOUT_SECONDS = 3

/**
 * Таймауты для картинок TMDB: соединение 3 с, чтение 5 с. У каждого кадра есть запасные источники (Tenrai, Kodik, yani),
 * поэтому при упавшем или зависшем узле CDN лучше быстро перейти к ним, чем ждать общие десять секунд.
 */
fun tmdbImageTimeouts(): Interceptor =
    HostTimeoutInterceptor(TMDB_IMAGE_HOSTS, TMDB_CONNECT_TIMEOUT_SECONDS, TMDB_READ_TIMEOUT_SECONDS)

/**
 * DNS общего OkHttp-клиента: хосты картинок TMDB при блокировке резолвятся через Quad9 (DNS-over-HTTPS, адрес
 * `9.9.9.9` задан сразу, поэтому для самого DoH обычный DNS не нужен), остальные — системным DNS.
 */
fun tmdbImageDns(): Dns {
    // Клиент и DoH создаются лениво: на старте приложения они не нужны, а без блокировки не понадобятся вовсе.
    val quad9 by lazy {
        DnsOverHttps.Builder()
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(DOH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                    .readTimeout(DOH_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                    .build(),
            )
            .url("https://dns.quad9.net/dns-query".toHttpUrl())
            .bootstrapDnsHosts(InetAddress.getByName("9.9.9.9"), InetAddress.getByName("149.112.112.112"))
            .build()
    }
    return BlockedHostDns(hosts = TMDB_IMAGE_HOSTS, fallback = Dns { hostname -> quad9.lookup(hostname) })
}
