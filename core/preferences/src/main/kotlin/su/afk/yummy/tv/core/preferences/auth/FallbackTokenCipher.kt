package su.afk.yummy.tv.core.preferences.auth

import android.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Запасной шифр для устройств с нерабочим keymaster: обфускация, а не криптография. Запись всё
 * так же лежит в app-private SharedPreferences, но её восстановление не зависит от системного
 * хранилища ключей. Компромисс осознанный — альтернатива на такой прошивке это неработающий вход.
 */
internal class FallbackTokenCipher : TokenCipher {

    override val mode: TokenStorageMode = TokenStorageMode.FALLBACK

    override fun encrypt(value: String): String {
        val masked = mask(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(masked, Base64.NO_WRAP)
    }

    override fun decrypt(value: String): String {
        val masked = Base64.decode(value, Base64.NO_WRAP)
        return String(mask(masked), StandardCharsets.UTF_8)
    }

    override fun selfTest(): Boolean = runCatching { decrypt(encrypt(SELF_TEST_VALUE)) }
        .getOrNull() == SELF_TEST_VALUE

    private fun mask(bytes: ByteArray): ByteArray =
        ByteArray(bytes.size) { index -> (bytes[index].toInt() xor MASK[index % MASK.size]).toByte() }

    private companion object {
        const val SELF_TEST_VALUE = "yummy"
        val MASK = intArrayOf(0x5A, 0x3C, 0x71, 0x2E, 0x69, 0x14, 0xA7, 0xD3)
    }
}
