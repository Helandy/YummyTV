package su.afk.yummy.tv.core.error

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.analytics.api.coroutine.ErrorCoroutineAnalytics
import su.afk.yummy.tv.core.error.api.ErrorDestinationFactory
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.error.api.isNetworkError
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

/** Default error mapper for network, HTTP, and generic application failures. */
internal class ErrorHandlerImpl @Inject constructor(
    private val strings: StringProvider,
    private val errorDestination: ErrorDestinationFactory,
    private val navigationManager: INavigationManager,
    private val errorCoroutineAnalytics: ErrorCoroutineAnalytics,
) : ErrorHandler {

    override fun parse(
        t: Throwable,
        navigate: Boolean,
        retryKey: String?,
        owner: String?,
    ): ErrorItem {
        if (t is CancellationException) throw t

        owner?.let {
            if (!t.isNetworkError()) {
                runCatching {
                    errorCoroutineAnalytics.reportCoroutineError(
                        owner = it,
                        throwable = t,
                    )
                }
            }
        }

        val base = when (t) {
            is ResponseException -> parseKtorResponse(t)

            else -> {
                when {
                    t is SocketTimeoutException -> ErrorItem(
                        title = strings.get(R.string.err_title_timeout),
                        message = strings.get(R.string.err_msg_timeout),
                        cause = t.toString()
                    )

                    t is IOException -> ErrorItem(
                        title = strings.get(R.string.err_title_no_connection),
                        message = strings.get(R.string.err_msg_no_connection),
                        cause = t.toString()
                    )

                    else -> ErrorItem(
                        title = strings.get(R.string.err_title_generic),
                        message = t.message?.takeIf { it.isNotBlank() }
                            ?: strings.get(R.string.err_msg_generic),
                        cause = t.toString()
                    )
                }
            }
        }

        val item = base.copy(retryKey = retryKey)

        if (navigate) navigationManager.navigate(errorDestination(item))

        return item
    }

    private fun parseKtorResponse(e: ResponseException): ErrorItem {
        val code = e.response.status.value
        val url = e.response.call.request.url.toString()
        val method = e.response.call.request.method.value

        val title = httpTitle(code)
        val fallback = httpMessage(code)

        return ErrorItem(
            title = title,
            message = fallback,
            code = code,
            fallback = fallback,
            url = url,
            method = method,
            cause = e.message,
        )
    }

    private fun httpTitle(code: Int): String = when (code) {
        401 -> strings.get(R.string.err_title_unauthorized)
        403 -> strings.get(R.string.err_title_forbidden)
        404 -> strings.get(R.string.err_title_not_found)
        429 -> strings.get(R.string.err_title_too_many_requests)
        500 -> strings.get(R.string.err_title_server_error)
        502 -> strings.get(R.string.err_title_bad_gateway_502)
        503 -> strings.get(R.string.err_title_service_unavailable_503)
        504 -> strings.get(R.string.err_title_gateway_timeout_504)
        else -> strings.get(R.string.err_title_http_generic, code)
    }

    private fun httpMessage(code: Int): String = when (code) {
        401 -> strings.get(R.string.err_msg_unauthorized)
        403 -> strings.get(R.string.err_msg_forbidden)
        404 -> strings.get(R.string.err_msg_not_found)
        429 -> strings.get(R.string.err_msg_too_many_requests)
        500 -> strings.get(R.string.err_msg_server_error)
        502 -> strings.get(R.string.err_msg_bad_gateway_502)
        503 -> strings.get(R.string.err_msg_service_unavailable_503)
        504 -> strings.get(R.string.err_msg_gateway_timeout_504)
        else -> strings.get(R.string.err_msg_http_generic)
    }
}
