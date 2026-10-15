package su.afk.yummy.tv.core.network.yummy

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Адрес API и токен подписи сайта YummyTV, удалённо заменяемые через feature-флаги.
 *
 * Значения применяются один раз при запуске ([apply]) и не меняются до конца процесса. Пустое,
 * небезопасное (не https) или совпадающее со встроенным значение игнорируется: остаётся то, что
 * заведено в приложении.
 */
@Singleton
class SiteApiConfig @Inject constructor() {

    @Volatile
    var baseUrl: String = YUMMY_TV_API_BASE_URL
        private set

    @Volatile
    private var remoteToken: String = ""

    fun apply(remoteBaseUrl: String, remoteToken: String) {
        normalizeRemoteBaseUrl(remoteBaseUrl)?.let { baseUrl = it }
        this.remoteToken = remoteToken.trim()
    }

    /** Токен с сайта, если он пришёл и отличается от встроенного; иначе [builtIn]. */
    fun effectiveToken(builtIn: String): String = remoteToken.ifBlank { builtIn }

    private fun normalizeRemoteBaseUrl(raw: String): String? {
        val value = raw.trim().trimEnd('/')
        if (!value.startsWith("https://", ignoreCase = true) || value.length <= "https://".length) return null
        if (value.any { it.isWhitespace() }) return null
        return value.takeUnless { it == YUMMY_TV_API_BASE_URL }
    }
}
