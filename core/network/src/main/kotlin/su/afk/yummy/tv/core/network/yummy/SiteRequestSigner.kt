package su.afk.yummy.tv.core.network.yummy

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.Url

/**
 * Подписывает запросы к API сайта YummyTV (заголовки `X-Sig*`). Реализация приходит из необязательного
 * модуля `private/sitesign`, который владеет секретом и форматом подписи; без него запросы уходят
 * неподписанными, и сервер их отклоняет.
 */
interface SiteRequestSigner {

    /** Есть ли чем подписывать: секрет сборки и отпечаток сертификата APK. */
    val isAvailable: Boolean

    /**
     * Заголовки для запроса; пусто, если подписать нельзя. [query] — строка запроса без `?` в том виде,
     * в каком она уйдёт на сервер; [body] должен совпадать с отправляемыми байтами.
     */
    fun headers(method: String, path: String, query: String, body: ByteArray): Map<String, String>
}

/**
 * Добавляет подпись [signers] к запросу на [url]. Вызывать после всех `parameter(...)`: строка запроса
 * берётся из уже собранного запроса и входит в подпись.
 */
fun HttpRequestBuilder.signSiteRequest(
    signers: Collection<SiteRequestSigner>,
    method: String,
    url: String,
    body: ByteArray = ByteArray(0),
) {
    val path = Url(url).encodedPath
    val query = this.url.build().encodedQuery
    signers.forEach { signer ->
        signer.headers(method, path, query, body).forEach { (name, value) -> header(name, value) }
    }
}

/** Объявляет набор подписчиков, чтобы он был доступен и при удалённом модуле с подписью. */
@Module
@InstallIn(SingletonComponent::class)
internal interface SiteRequestSignerModule {

    @Multibinds
    fun signers(): Set<SiteRequestSigner>
}
