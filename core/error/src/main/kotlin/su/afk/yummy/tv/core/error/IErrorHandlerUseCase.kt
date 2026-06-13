package su.afk.yummy.tv.core.error

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.commonscreen.navigator.IErrorNavigator
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

/** Converts thrown errors into user-facing error items and optionally opens the error screen. */
interface IErrorHandlerUseCase {
    fun parse(t: Throwable, navigate: Boolean = false, retryKey: String? = null): ErrorItem
}

/** Default error mapper for network, HTTP, and generic application failures. */
class ErrorHandlerUseCaseImpl @Inject constructor(
    private val strings: StringProvider,
    private val errorNavigator: IErrorNavigator,
    private val navigationManager: NavigationManager,
) : IErrorHandlerUseCase {

    override fun parse(t: Throwable, navigate: Boolean, retryKey: String?): ErrorItem {
        if (t is CancellationException) throw t

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

        if (navigate) navigationManager.navigate(errorNavigator(item))

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
