package su.afk.yummy.tv.core.network.dns

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException

/** Хост картинок недавно не отвечал, повторный запрос не делался: клиент сразу переходит к следующему источнику. */
class HostTemporarilyDownException(host: String) : IOException("$host не отвечает, повтор позже")

/**
 * Память о сбое хоста для запросов, подходящих под [isTarget] (картинки с запасными источниками).
 *
 * Зависший хост (TLS-рукопожатие не завершается, соединение не устанавливается) стоит каждой картинке полного
 * таймаута, и список с десятками постеров показывает их по одному, пока каждый ждёт свои секунды. После
 * [failureThreshold] неудач подряд хост считается упавшим на [downMillis]: запросы к нему сразу получают
 * [HostTemporarilyDownException], а Coil переходит к следующему адресу из списка. По истечении срока пропускается один
 * пробный запрос: успех снимает отметку, неудача продлевает её. Любой успешный ответ обнуляет счётчик неудач.
 *
 * Неудачей считаются только таймаут, отказ соединения и сбой TLS. Обрыв соединения (его даёт сворачивание приложения),
 * отмена запроса, ошибка DNS (у неё своя память в [BlockedHostDns]) и HTTP-статусы хост не помечают.
 */
class HostFailureMemoryInterceptor(
    private val isTarget: (HttpUrl) -> Boolean,
    private val failureThreshold: Int = DEFAULT_FAILURE_THRESHOLD,
    private val downMillis: Long = DEFAULT_DOWN_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : Interceptor {

    private class HostState {
        var consecutiveFailures = 0

        /** 0 — хост считается живым; иначе момент, до которого его не трогаем. После этого момента идёт пробный запрос. */
        var downUntilMs = 0L
        var probing = false
    }

    private val states = ConcurrentHashMap<String, HostState>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isTarget(request.url)) return chain.proceed(request)

        val host = request.url.host.lowercase()
        val state = states.getOrPut(host) { HostState() }
        val isProbe = synchronized(state) {
            if (state.downUntilMs == 0L) return@synchronized false
            if (state.downUntilMs > nowMillis() || state.probing) throw HostTemporarilyDownException(host)
            state.probing = true
            true
        }

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            synchronized(state) {
                if (isProbe) state.probing = false
                if (!chain.call().isCanceled() && e.isHostFailure()) {
                    state.consecutiveFailures++
                    if (isProbe || state.consecutiveFailures >= failureThreshold) state.downUntilMs = nowMillis() + downMillis
                }
            }
            throw e
        }
        synchronized(state) {
            state.consecutiveFailures = 0
            state.downUntilMs = 0L
            if (isProbe) state.probing = false
        }
        return response
    }

    private fun IOException.isHostFailure(): Boolean =
        this is SocketTimeoutException || this is ConnectException || this is NoRouteToHostException || this is SSLException

    private companion object {
        const val DEFAULT_FAILURE_THRESHOLD = 2
        const val DEFAULT_DOWN_MILLIS = 60 * 1000L
    }
}
