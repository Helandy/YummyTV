package su.afk.yummy.tv.core.designsystem.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Текст ошибки загрузки для UI (Paging `LoadState.Error` и т.п.): сетевые сбои —
 * локализованным сообщением, а исходный текст (`Unable to resolve host ...`) второй строкой,
 * чтобы было видно, с каким хостом проблема.
 */
@Composable
fun Throwable.uiMessage(): String = when (this) {
    is SocketTimeoutException -> withCause(stringResource(R.string.error_msg_timeout))
    is IOException -> withCause(stringResource(R.string.error_msg_no_connection))
    else -> message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_msg_generic)
}

private fun Throwable.withCause(text: String): String =
    message?.takeIf { it.isNotBlank() }?.let { "$text\n$it" } ?: text
