package su.afk.yummy.tv.source.anime4k.data.signing

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class SecretDecoderTest : BaseUnitTest() {

    @Test
    fun `decode restores the masked secret`() {
        val key = byteArrayOf(0x5a, 0x13, -0x21)
        val secret = "vector-secret"
        val masked = ByteArray(secret.length) { (secret[it].code xor key[it % key.size].toInt()).toByte() }

        assertEquals(secret, SecretDecoder.decode(masked, key))
    }
}
