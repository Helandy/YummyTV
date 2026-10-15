package su.afk.yummy.tv.source.anime4k.data.signing

import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner

/** Подписывает запросы к серверу YummyTV: заголовки `X-Sig*`. Без секрета сборки подписи нет, источник скрыт. */
internal interface RequestSigner : SiteRequestSigner {
    val isAvailable: Boolean

    /** Заголовки для запроса; пусто, если подписать нельзя. [body] должен совпадать с отправляемыми байтами. */
    override fun headers(method: String, path: String, body: ByteArray): Map<String, String>
}
