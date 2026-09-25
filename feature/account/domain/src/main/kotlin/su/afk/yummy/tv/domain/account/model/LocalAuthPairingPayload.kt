package su.afk.yummy.tv.domain.account.model

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Содержимое QR-кода сопряжения, который ТВ показывает рядом с текстовым кодом.
 *
 * IP и порт в QR не кладём: у приставки бывает несколько интерфейсов, а NSD-поиск на телефоне
 * всё равно работает и находит ТВ по имени сервиса.
 *
 * @property deviceId NSD-имя сервиса ТВ (совпадает с [DiscoveredDevice.id]); `null`, если
 * отсканирован голый код без адресата.
 * @property code Нормализованный код сопряжения длиной [LocalAuthCode.LENGTH].
 */
data class LocalAuthPairingPayload(
    val deviceId: String?,
    val code: String,
) {
    companion object {
        private const val PREFIX = "yummytv://pair?"
        private const val PARAM_DEVICE = "d"
        private const val PARAM_CODE = "c"
        private const val CHARSET = "UTF-8"

        fun encode(deviceId: String, code: String): String =
            "$PREFIX$PARAM_DEVICE=${URLEncoder.encode(deviceId, CHARSET)}" +
                "&$PARAM_CODE=${URLEncoder.encode(code, CHARSET)}"

        /** @return `null`, если в тексте нет валидного кода сопряжения. */
        fun parse(raw: String): LocalAuthPairingPayload? {
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
         * текста (чужой URL) normalize может случайно выцепить десять «валидных» символов.
         */
        private fun String.toValidCode(): String? {
            if (length > LocalAuthCode.LENGTH + MAX_SEPARATORS) return null
            return LocalAuthCode.normalize(this).takeIf { it.length == LocalAuthCode.LENGTH }
        }

        private const val MAX_SEPARATORS = 2
    }
}
