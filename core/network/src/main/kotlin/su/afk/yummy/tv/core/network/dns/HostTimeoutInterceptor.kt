package su.afk.yummy.tv.core.network.dns

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Короткие таймауты соединения и чтения для указанных [hosts]. Общий клиент рассчитан на медленные плееры и API
 * (десять секунд на каждый этап), а для картинки с CDN, у которой есть запасные источники, это слишком долго: упавший
 * или зависший узел должен отваливаться за секунды, и цепочка уходит на следующий источник. Остальные хосты не трогаем.
 */
class HostTimeoutInterceptor(
    private val hosts: Set<String>,
    private val connectTimeoutSeconds: Int,
    private val readTimeoutSeconds: Int,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.host !in hosts) return chain.proceed(chain.request())
        return chain
            .withConnectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .withReadTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .proceed(chain.request())
    }
}
