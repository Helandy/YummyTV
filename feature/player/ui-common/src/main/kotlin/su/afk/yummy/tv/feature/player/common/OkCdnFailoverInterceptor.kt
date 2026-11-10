package su.afk.yummy.tv.feature.player.common

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import su.afk.yummy.tv.core.common.player.OkCdnHostFailover

/**
 * Прозрачный переезд okcdn на резервный узел внутри HTTP-запроса.
 *
 * Основной узел в непредсказуемый момент начинает отвечать `400`/`403` на новые соединения, хотя
 * подпись жива. Файл читается одним долгим запросом, поэтому отказ случается после паузы: старое
 * соединение умерло, а новое узел не принимает. Без перехватчика загрузчик получал фатал, плеер
 * доигрывал буфер до конца и только тогда падал в переезд со сбросом буфера и спиннером.
 *
 * Здесь запрос с отказом повторяется на резервном узле, и плеер ответ принимает как обычный. Узел,
 * отвечавший отказом, запоминается: следующие запросы идут на резервный сразу. Если отказывает и
 * резервный, его ответ возвращается как есть, дальше работает переезд на уровне состояния плеера
 * (`DefaultSourceBehavior`). Механика целиком — в `docs/cvh-player.md`.
 */
internal class OkCdnFailoverInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val failoverHost = OkCdnHostFailover.failoverFor(host) ?: return chain.proceed(request)

        if (OkCdnHostFailover.isDead(host)) {
            return chain.proceed(request.onHost(failoverHost))
        }
        val response = chain.proceed(request)
        if (response.code !in FAILOVER_RESPONSE_CODES) return response

        response.close()
        OkCdnHostFailover.markDead(host)
        return chain.proceed(request.onHost(failoverHost))
    }

    private fun Request.onHost(host: String): Request =
        newBuilder().url(url.newBuilder().host(host).build()).build()

    private companion object {
        // Набор совпадает с `DefaultSourceBehavior.FAILOVER_HTTP_CODES`: те же отказы узла.
        val FAILOVER_RESPONSE_CODES = setOf(400, 403)
    }
}
