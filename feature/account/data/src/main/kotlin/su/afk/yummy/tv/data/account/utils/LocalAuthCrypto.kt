package su.afk.yummy.tv.data.account.utils

import android.util.Base64
import su.afk.yummy.tv.domain.account.model.LocalAuthCode
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Шифрование сессии для передачи по локальной сети.
 *
 * Ключ выводится из кода сопряжения ([LocalAuthCode]) — PBKDF2 со случайной солью и большим
 * числом итераций. Сам код длинный не просто так: шифртекст может оказаться у чужого в той же
 * сети, а перебирать его он будет офлайн, где лимит попыток на ТВ уже ничего не ограничивает.
 */
object LocalAuthCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val TAG_LENGTH = 128
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val ITERATIONS = 200_000

    private val secureRandom = SecureRandom()

    /** Криптостойкий код сопряжения в алфавите [LocalAuthCode]. */
    fun generatePin(): String {
        val alphabet = LocalAuthCode.ALPHABET
        val chars = CharArray(LocalAuthCode.LENGTH) { alphabet[secureRandom.nextInt(alphabet.length)] }
        return String(chars)
    }

    /** @return зашифрованные данные, IV и соль — всё в Base64. */
    fun encrypt(data: String, pin: String): EncryptedPayload {
        val salt = ByteArray(SALT_LENGTH).also(secureRandom::nextBytes)
        val key = deriveKey(pin, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            data = encrypted.toBase64(),
            iv = cipher.iv.toBase64(),
            salt = salt.toBase64(),
        )
    }

    /** @throws javax.crypto.AEADBadTagException если PIN неверный или данные подменены. */
    fun decrypt(encryptedData: String, ivBase64: String, saltBase64: String, pin: String): String {
        val key = deriveKey(pin, saltBase64.fromBase64())
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, ivBase64.fromBase64()))

        return String(cipher.doFinal(encryptedData.fromBase64()), Charsets.UTF_8)
    }

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    data class EncryptedPayload(val data: String, val iv: String, val salt: String)
}
