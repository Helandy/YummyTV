package su.afk.yummy.tv.core.network.yummy

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.Url

/**
 * Подписывает запросы к API сайта YummyTV (заголовки `X-Sig*`). Реализация приходит из модуля,
 * который владеет секретом подписи; без неё запросы уходят неподписанными.
 */
interface SiteRequestSigner {

    /** Заголовки для запроса; пусто, если подписать нельзя. [body] должен совпадать с отправляемыми байтами. */
    fun headers(method: String, path: String, body: ByteArray): Map<String, String>
}

/** Добавляет подпись [signers] к запросу на [url]; подписывается только путь, как ожидает сервер. */
fun HttpRequestBuilder.signSiteRequest(
    signers: Collection<SiteRequestSigner>,
    method: String,
    url: String,
    body: ByteArray = ByteArray(0),
) {
    val path = Url(url).encodedPath
    signers.forEach { signer ->
        signer.headers(method, path, body).forEach { (name, value) -> header(name, value) }
    }
}

/** Объявляет набор подписчиков, чтобы он был доступен и при удалённом модуле с подписью. */
@Module
@InstallIn(SingletonComponent::class)
internal interface SiteRequestSignerModule {

    @Multibinds
    fun signers(): Set<SiteRequestSigner>
}
