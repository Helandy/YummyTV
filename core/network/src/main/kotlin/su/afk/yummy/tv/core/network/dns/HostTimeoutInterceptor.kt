package su.afk.yummy.tv.core.network.dns

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Короткие таймауты соединения и чтения для запросов, подходящих под [isTarget]. Общий клиент рассчитан на медленные плееры и API
 * (десять секунд на каждый этап), а для картинки с CDN, у которой есть запасные источники, это слишком долго: упавший
 * или зависший узел должен отваливаться за секунды, и цепочка уходит на следующий источник. Остальные запросы не трогаем.
 */
class HostTimeoutInterceptor(
    private val isTarget: (HttpUrl) -> Boolean,
    private val connectTimeoutSeconds: Int,
    private val readTimeoutSeconds: Int,
) : Interceptor {

    constructor(hosts: Set<String>, connectTimeoutSeconds: Int, readTimeoutSeconds: Int) :
        this({ url -> url.host in hosts }, connectTimeoutSeconds, readTimeoutSeconds)

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isTarget(chain.request().url)) return chain.proceed(chain.request())
        return chain
            .withConnectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .withReadTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .proceed(chain.request())
    }
}
