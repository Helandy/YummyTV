package su.afk.yummy.tv.source.moon.data.utils

import kotlin.io.encoding.Base64

/** Страница Moon для тестов: скрипт плеера, зашифрованный так же, как на сайте. */
internal object MoonTestPage {

    /** Обратное преобразование к [decodeMoonPayload]: байт состояния, 32 байта ключа, шифртекст. */
    fun encodePayload(plain: String): String {
        val header = ByteArray(33) { (it * 7 + 3).toByte() }
        var state = header[0].toInt() and 0xFF
        val body = plain.encodeToByteArray()
        val out = header + ByteArray(body.size)
        for (i in body.indices) {
            val key = header[1 + i % 32].toInt() and 0xFF
            val cipher = (body[i].toInt() and 0xFF) xor state xor key
            out[33 + i] = cipher.toByte()
            state = (cipher + key) and 0xFF
        }
        return Base64.Default.encode(out)
    }

    fun xorFile(file: String, key: String): String {
        val k = key.encodeToByteArray()
        val raw = file.encodeToByteArray()
        return Base64.Default.encode(ByteArray(raw.size) { (raw[it].toInt() xor k[it % k.size].toInt()).toByte() })
    }

    fun page(file: String, key: String = "s3cr3t"): String {
        val script = "var k = \"$key\"; player({file: f(\"${xorFile(file, key)}\")});"
        return "<script>eval(atob(\"${encodePayload(script)}\"))</script>"
    }
}
