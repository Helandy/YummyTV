package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.model.error.isNetworkError

private val NETWORK_ERROR_TYPE_PREFIXES = listOf(
    "ConnectException",
    "UnknownHostException",
    "SocketTimeoutException",
    "SocketException",
    "NoRouteToHostException",
    "SSLException",
    "SSLHandshakeException",
    "ConnectTimeoutException",
    "HttpRequestTimeoutException",
    "UnresolvedAddressException",
    "EOFException",
)

private val NETWORK_ERROR_MESSAGE_MARKERS = listOf(
    "timeout has expired",
    "timed out",
    "unable to resolve host",
    "failed to connect",
    "connection reset",
    "connection refused",
    "unexpected end of stream",
)

/**
 * Сбой резолва потока вызван сетью пользователя, а не багом приложения. Экстракторы кладут
 * исключение в [Throwable] только иногда; чаще оно уже свёрнуто в строку вида
 * `"ConnectException: Failed to connect to host"`, поэтому проверяем и тип, и префикс сообщения.
 */
fun isNetworkResolveFailure(throwable: Throwable?, message: String?): Boolean =
    throwable?.isNetworkError() == true ||
        message?.trimStart()?.let { text ->
            NETWORK_ERROR_TYPE_PREFIXES.any { text.startsWith("$it:") || text == it } ||
                NETWORK_ERROR_MESSAGE_MARKERS.any { text.contains(it, ignoreCase = true) }
        } == true
