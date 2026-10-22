package su.afk.yummy.tv.buildlogic

import org.gradle.api.Project
import java.security.MessageDigest
import java.util.Properties
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Секрет из `local.properties` для `buildConfigField("String", ...)`, уже в кавычках.
 * Не найден — пустая строка: сборка проходит, соответствующий SDK не инициализируется.
 */
fun Project.buildConfigSecret(property: String): String {
    val value = rootProject.localProperties().getProperty(property).orEmpty()
    return value.toBuildConfigString()
}

/**
 * Секрет для `buildConfigField("String", ...)` в виде Java-выражения: четыре XOR-доли и соль
 * передаются в [decoderFqn] `.decode(byte[], byte[], byte[], byte[], byte[])`. Открытой строки и
 * готового ключа в константах класса нет, `strings`/grep/jadx по APK секрет не находят. Доли и соль
 * выводятся из самого секрета (HMAC), поэтому сборка воспроизводима, а ключ не берётся из публичных
 * строк. Внутри маски лежит 4-байтовая контрольная сумма: декодер отдаёт пустую строку, если секрет
 * испорчен. Это не шифрование, а барьер от поверхностного извлечения. Алгоритм общий с `SecretDecoder`.
 */
fun Project.buildConfigObfuscatedSecret(property: String, decoderFqn: String): String {
    val value = rootProject.localProperties().getProperty(property).orEmpty()
    if (value.isEmpty()) return "\"\""
    val secret = value.toByteArray()
    val salt = hmac(secret, "salt")
    val payload = secret + MessageDigest.getInstance("SHA-256").digest(secret).copyOf(CHECK_SIZE)
    val stream = keystream(salt, payload.size)
    val masked = ByteArray(payload.size) { (payload[it].toInt() xor stream[it].toInt()).toByte() }
    val shares = MutableList(SHARES - 1) { hmac(secret, "share$it", masked.size) }
    val last = ByteArray(masked.size) { i ->
        shares.fold(masked[i].toInt()) { acc, share -> acc xor share[i].toInt() }.toByte()
    }
    shares += last
    val args = (shares + salt).joinToString(", ") { "new byte[]{${it.joinToString(",")}}" }
    return "$decoderFqn.decode($args)"
}

private const val SHARES = 4
private const val CHECK_SIZE = 4

private fun hmac(key: ByteArray, label: String, size: Int = 32): ByteArray = keystream(key, size, label)

/** HMAC-SHA256 в режиме счётчика: блок i = HMAC(key, label | i). */
private fun keystream(key: ByteArray, size: Int, label: String = ""): ByteArray {
    val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
    val out = ArrayList<Byte>(size)
    var counter = 0
    while (out.size < size) {
        mac.update(label.toByteArray())
        mac.update(counter++.toByte())
        out += mac.doFinal().toList()
    }
    return out.take(size).toByteArray()
}

private fun Project.localProperties(): Properties = Properties().apply {
    file("local.properties").takeIf { it.isFile }?.reader()?.use(::load)
}

private fun String.toBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
