package su.afk.yummy.tv.source.anime4k.data.signing

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SecretDecoderTest : BaseUnitTest() {

    @Test
    fun `decode restores the secret from shares`() {
        val secret = "vector-secret"
        val (shares, salt) = encode(secret)

        assertEquals(secret, SecretDecoder.decode(shares[0], shares[1], shares[2], shares[3], salt))
    }

    @Test
    fun `decode returns empty string when a share is corrupted`() {
        val (shares, salt) = encode("vector-secret")
        shares[1][0] = (shares[1][0].toInt() xor 1).toByte()

        assertEquals("", SecretDecoder.decode(shares[0], shares[1], shares[2], shares[3], salt))
    }

    /** Зеркало кодировщика из build-logic (`buildConfigObfuscatedSecret`). */
    private fun encode(secretText: String): Pair<List<ByteArray>, ByteArray> {
        val secret = secretText.toByteArray()
        val salt = stream(secret, 32, "salt")
        val payload = secret + MessageDigest.getInstance("SHA-256").digest(secret).copyOf(4)
        val ks = stream(salt, payload.size, "")
        val masked = ByteArray(payload.size) { (payload[it].toInt() xor ks[it].toInt()).toByte() }
        val shares = MutableList(3) { stream(secret, masked.size, "share$it") }
        shares += ByteArray(masked.size) { i ->
            shares.fold(masked[i].toInt()) { acc, s -> acc xor s[i].toInt() }.toByte()
        }
        return shares to salt
    }

    private fun stream(key: ByteArray, size: Int, label: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
        val out = ArrayList<Byte>()
        var counter = 0
        while (out.size < size) {
            mac.update(label.toByteArray())
            mac.update(counter++.toByte())
            out += mac.doFinal().toList()
        }
        return out.take(size).toByteArray()
    }
}
