package su.afk.yummy.tv.core.designsystem.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.ktor.client.plugins.ResponseException
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.model.text.UiText
import java.io.IOException

/** Разрешает [UiText] в строку по текущей локали: вызывается при отрисовке, а не во ViewModel. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> stringResource(resId, *args.toTypedArray())
    is UiText.Failure -> when {
        // Сеть и HTTP-ответы — локализованным сообщением (см. uiMessage), исходный текст не показываем сырым.
        cause is IOException || cause is ResponseException -> cause.uiMessage()
        else -> cause.message?.takeIf { it.isNotBlank() }
            ?: fallback?.asString()
            ?: stringResource(R.string.error_msg_generic)
    }
}
