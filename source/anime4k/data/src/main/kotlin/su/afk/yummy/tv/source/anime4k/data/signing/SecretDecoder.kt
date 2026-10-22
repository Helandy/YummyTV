package su.afk.yummy.tv.source.anime4k.data.signing

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Собирает секрет сборки из четырёх XOR-долей и соли. Вызывается из сгенерированного `BuildConfig`
 * (см. `buildConfigObfuscatedSecret` в build-logic), поэтому объект публичный; алгоритм зеркален
 * кодировщику. Контрольная сумма не сошлась — пустая строка, и подпись отключается.
 */
object SecretDecoder {

    private const val CHECK_SIZE = 4

    @JvmStatic
    fun decode(a: ByteArray, b: ByteArray, c: ByteArray, d: ByteArray, salt: ByteArray): String {
        val stream = keystream(salt, a.size)
        val payload = ByteArray(a.size) { i ->
            (a[i].toInt() xor b[i].toInt() xor c[i].toInt() xor d[i].toInt() xor stream[i].toInt()).toByte()
        }
        val secret = payload.copyOf(payload.size - CHECK_SIZE)
        val check = MessageDigest.getInstance("SHA-256").digest(secret).copyOf(CHECK_SIZE)
        val valid = MessageDigest.isEqual(check, payload.copyOfRange(secret.size, payload.size))
        return if (valid) String(secret) else ""
    }

    private fun keystream(key: ByteArray, size: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
        val out = ArrayList<Byte>(size)
        var counter = 0
        while (out.size < size) {
            mac.update(counter++.toByte())
            out += mac.doFinal().toList()
        }
        return out.take(size).toByteArray()
    }
}
