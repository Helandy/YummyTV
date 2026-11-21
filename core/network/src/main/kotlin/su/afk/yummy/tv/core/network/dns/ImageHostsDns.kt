package su.afk.yummy.tv.core.network.dns

import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import su.afk.yummy.tv.core.network.NetworkTimeouts
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/** CDN картинок TMDB: у части провайдеров его DNS-имена подменены на `127.0.0.1`, а узлы бывают медленными или упавшими. */
private val TMDB_IMAGE_HOSTS = setOf("image.tmdb.org", "media.themoviedb.org")

/** Домен yani: на его поддоменах лежат картинки (постеры, баннеры, аватары), и их хост yani уже менял (`static` → `imgproxy`). */
private const val YANI_DOMAIN_SUFFIX = ".yani.tv"

/** API yani живёт на том же домене, но идёт через общий клиент с обычными таймаутами и без DoH: к нему правила картинок не применяем. */
private const val YANI_API_HOST = "api.yani.tv"

/**
 * Хост картинок, у которого есть запасные источники: CDN TMDB и любой поддомен `yani.tv`, кроме API. Поддомены не перечисляем
 * по именам: когда yani снова переедет на другой хост картинок, короткие таймауты и запасной DNS сработают сами.
 */
internal fun isImageCdnHost(host: String): Boolean {
    val name = host.lowercase()
    return name in TMDB_IMAGE_HOSTS || (name.endsWith(YANI_DOMAIN_SUFFIX) && name != YANI_API_HOST)
}

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "avif", "gif")

/**
 * Запрос картинки у хоста с запасными источниками: хост из [isImageCdnHost] или свой [staticHost] и путь с расширением
 * картинки. Поддомены yani не только про картинки (`alloha.yani.tv` отдаёт страницу плеера), и таймауты с памятью
 * о сбое должны касаться только картинок, а не остальных запросов к тому же домену.
 */
internal fun isImageCdnRequest(url: HttpUrl, staticHost: String): Boolean {
    val host = url.host
    val knownHost = isImageCdnHost(host) || (staticHost.isNotBlank() && host.equals(staticHost, ignoreCase = true))
    return knownHost && url.encodedPath.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
}

/**
 * Таймауты для картинок: соединение 3 с, чтение 5 с. Для CDN TMDB, CDN картинок yani (любой поддомен `yani.tv`, кроме API) и для своего хоста статики [staticHost] (копии кадров
 * и постеров). У каждой картинки есть запасные источники (Tenrai, Kodik, yani, TMDB, наш хост), поэтому при упавшем или зависшем
 * узле лучше быстро перейти к ним, чем ждать общие десять секунд. Пустой [staticHost] — только чужие CDN.
 */
fun imageHostTimeouts(staticHost: String): Interceptor =
    HostTimeoutInterceptor(
        isTarget = { url -> isImageCdnRequest(url, staticHost) },
        connectTimeoutSeconds = NetworkTimeouts.IMAGE_HOST_CONNECT_SECONDS,
        readTimeoutSeconds = NetworkTimeouts.IMAGE_HOST_READ_SECONDS,
    )

/**
 * Память о сбое хоста картинок: см. [HostFailureMemoryInterceptor]. Цели те же, что у [imageHostTimeouts]: картинки CDN TMDB,
 * любого поддомена `yani.tv`, кроме API, и своего хоста статики [staticHost].
 */
fun imageHostFailureMemory(staticHost: String): Interceptor =
    HostFailureMemoryInterceptor(isTarget = { url -> isImageCdnRequest(url, staticHost) })

/**
 * DNS общего OkHttp-клиента: хосты картинок TMDB и yani (любые поддомены `yani.tv`, кроме API) при блокировке или сбое DNS резолвятся через Quad9 (DNS-over-HTTPS, адрес
 * `9.9.9.9` задан сразу, поэтому для самого DoH обычный DNS не нужен), остальные — системным DNS.
 */
fun imageHostsDns(): Dns {
    // Клиент и DoH создаются лениво: на старте приложения они не нужны, а без блокировки не понадобятся вовсе.
    val quad9 by lazy {
        DnsOverHttps.Builder()
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(NetworkTimeouts.DOH_SECONDS.toLong(), TimeUnit.SECONDS)
                    .readTimeout(NetworkTimeouts.DOH_SECONDS.toLong(), TimeUnit.SECONDS)
                    .build(),
            )
            .url("https://dns.quad9.net/dns-query".toHttpUrl())
            .bootstrapDnsHosts(InetAddress.getByName("9.9.9.9"), InetAddress.getByName("149.112.112.112"))
            .build()
    }
    return BlockedHostDns(isTargetHost = ::isImageCdnHost, fallback = Dns { hostname -> quad9.lookup(hostname) })
}
