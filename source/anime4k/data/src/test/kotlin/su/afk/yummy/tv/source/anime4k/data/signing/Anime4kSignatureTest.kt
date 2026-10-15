package su.afk.yummy.tv.source.anime4k.data.signing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/** Вектор общий с сервером (`Anime4kSignatureTest` в YummyTV_API): менять только вместе. */
class Anime4kSignatureTest : BaseUnitTest() {

    private val secret = "vector-secret"
    private val cert = "11".repeat(32)
    private val body = """{"ticket":"t","malId":1735}"""
    private val ts = "1792000000"
    private val nonce = "00112233445566778899aabbccddeeff"

    @Test
    fun `reference vector matches the server`() {
        val key = Anime4kSignature.signingKey(secret, cert)

        assertEquals(
            "224823456053ea4bff9bc3278d08c1d8fa98c517635e547f7558084221780a79",
            key.joinToString("") { "%02x".format(it) },
        )
        assertEquals(
            "4ce80cd49f7f17528000cc3d75686a2dd36c8479ce733dd61cf7f9eb1555e822",
            Anime4kSignature.sign(key, "POST", "/api/anime4k/streams", ts, nonce, body.toByteArray()),
        )
        assertEquals(
            "69aa4ba8b8d10d949e8e934b2646dae62da631b7d20f5b039c31a460ca39cf41",
            Anime4kSignature.sign(key, "get", "/api/anime4k/title/1735", ts, nonce, ByteArray(0)),
        )
    }

    @Test
    fun `headers carry timestamp nonce signature and version`() {
        val signer = SignatureHeaders(secret, cert, "1.2.3", { 1_792_000_000L }, { nonce })

        val headers = signer.headers("POST", "/api/anime4k/streams", body.toByteArray())

        assertTrue(signer.isAvailable)
        assertEquals("1792000000", headers["X-Sig-Ts"])
        assertEquals(nonce, headers["X-Sig-Nonce"])
        assertEquals("1.2.3", headers["X-App-Version"])
        assertEquals("4ce80cd49f7f17528000cc3d75686a2dd36c8479ce733dd61cf7f9eb1555e822", headers["X-Sig"])
    }

    @Test
    fun `another certificate gives another signature`() {
        val official = SignatureHeaders(secret, cert, "v", { 1L }, { nonce })
        val repackaged = SignatureHeaders(secret, "22".repeat(32), "v", { 1L }, { nonce })

        assertNotEquals(
            official.headers("GET", "/p", ByteArray(0))["X-Sig"],
            repackaged.headers("GET", "/p", ByteArray(0))["X-Sig"],
        )
    }

    @Test
    fun `no secret or no certificate means nothing is signed`() {
        val noSecret = SignatureHeaders("", cert, "v", { 1L }, { nonce })
        val noCert = SignatureHeaders(secret, null, "v", { 1L }, { nonce })

        assertTrue(!noSecret.isAvailable && !noCert.isAvailable)
        assertTrue(noSecret.headers("GET", "/p", ByteArray(0)).isEmpty())
        assertTrue(noCert.headers("GET", "/p", ByteArray(0)).isEmpty())
    }
}
