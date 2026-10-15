package su.afk.yummy.tv.source.anime4k.data.signing

/** Чистая реализация без Android: время и нонс подставляются снаружи, поэтому проверяется на JVM. */
internal class SignatureHeaders(
    private val secret: String,
    private val certSha256Hex: String?,
    private val appVersion: String,
    private val nowSeconds: () -> Long,
    private val nonce: () -> String,
) : RequestSigner {

    override val isAvailable: Boolean get() = secret.isNotBlank() && !certSha256Hex.isNullOrBlank()

    override fun headers(method: String, path: String, body: ByteArray): Map<String, String> {
        val cert = certSha256Hex
        if (secret.isBlank() || cert.isNullOrBlank()) return emptyMap()
        val ts = nowSeconds().toString()
        val nonceValue = nonce()
        val key = Anime4kSignature.signingKey(secret, cert)
        return mapOf(
            "X-Sig-Ts" to ts,
            "X-Sig-Nonce" to nonceValue,
            "X-Sig" to Anime4kSignature.sign(key, method, path, ts, nonceValue, body),
            "X-App-Version" to appVersion,
        )
    }
}
