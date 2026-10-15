package su.afk.yummy.tv.source.anime4k.data.signing

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Формат подписи v1, общий с сервером (YummyTV_API `Anime4kSignature`): менять только вместе с ним.
 * key = HMAC(secret, certSha256Hex); signature = HMAC(key, "v1\nMETHOD\nPATH\nts\nnonce\nsha256hex(body)").
 */
internal object Anime4kSignature {
    private const val ALGORITHM = "HmacSHA256"

    fun signingKey(secret: String, certSha256Hex: String): ByteArray =
        hmac(secret.toByteArray(), certSha256Hex.toByteArray())

    fun sign(key: ByteArray, method: String, path: String, ts: String, nonce: String, body: ByteArray): String {
        val message = listOf("v1", method.uppercase(), path, ts, nonce, sha256Hex(body)).joinToString("\n")
        return hmac(key, message.toByteArray()).toHex()
    }

    fun sha256Hex(data: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(data).toHex()

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(key, ALGORITHM)) }.doFinal(data)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
