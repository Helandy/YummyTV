package su.afk.yummy.tv.domain.account.utils

import su.afk.yummy.tv.domain.account.model.LocalAuthCode
import su.afk.yummy.tv.domain.account.model.LocalAuthPairingPayload
import java.net.URLDecoder
import java.net.URLEncoder

private const val PREFIX = "yummytv://pair?"
private const val PARAM_DEVICE = "d"
private const val PARAM_CODE = "c"
private const val CHARSET = "UTF-8"
private const val MAX_SEPARATORS = 2

/** Содержимое QR-кода сопряжения для ТВ с именем сервиса [deviceId] и кодом [code]. */
fun encodeLocalAuthPairingPayload(deviceId: String, code: String): String =
    "$PREFIX$PARAM_DEVICE=${URLEncoder.encode(deviceId, CHARSET)}" +
        "&$PARAM_CODE=${URLEncoder.encode(code, CHARSET)}"

/** @return `null`, если в тексте нет валидного кода сопряжения. */
fun parseLocalAuthPairingPayload(raw: String): LocalAuthPairingPayload? {
    val text = raw.trim()
    if (!text.startsWith(PREFIX, ignoreCase = true)) {
        return text.toValidCode()?.let { LocalAuthPairingPayload(deviceId = null, code = it) }
    }
    val params = text.substring(PREFIX.length)
        .split('&')
        .mapNotNull { pair ->
            val key = pair.substringBefore('=', missingDelimiterValue = "")
            if (key.isEmpty()) return@mapNotNull null
            key to runCatching { URLDecoder.decode(pair.substringAfter('='), CHARSET) }
                .getOrNull()
        }
        .toMap()
    val code = params[PARAM_CODE]?.toValidCode() ?: return null
    val deviceId = params[PARAM_DEVICE]?.takeIf { it.isNotBlank() }
    return LocalAuthPairingPayload(deviceId = deviceId, code = code)
}

/**
 * Голый код допускаем с разделителем между группами, но не длиннее: иначе из произвольного
 * текста (чужой URL) normalize может случайно выцепить двенадцать «валидных» символов.
 */
private fun String.toValidCode(): String? {
    if (length > LocalAuthCode.LENGTH + MAX_SEPARATORS) return null
    return LocalAuthCode.normalize(this).takeIf { it.length == LocalAuthCode.LENGTH }
}
