package su.afk.yummy.tv.core.network.yani

import io.ktor.client.plugins.logging.Logger

/**
 * Прячет секреты в телах запросов и ответов, которые Ktor пишет на уровне BODY,
 * чтобы логин, пароль и токены не попадали в logcat и экспортируемые логи.
 */
internal class SensitiveDataMaskingLogger(private val delegate: Logger) : Logger {

    override fun log(message: String) {
        delegate.log(message.maskSensitiveJsonValues())
    }
}

private const val MASK = "***"

private val sensitiveJsonValue = Regex(
    "\"(password|login|email|token|access_token|refresh_token|encryptedToken|hash|" +
        "recaptcha_response|g-recaptcha-response)\"\\s*:\\s*\"(?:[^\"\\\\]|\\\\.)*\"",
)

private fun String.maskSensitiveJsonValues(): String =
    replace(sensitiveJsonValue) { "\"${it.groupValues[1]}\":\"$MASK\"" }
