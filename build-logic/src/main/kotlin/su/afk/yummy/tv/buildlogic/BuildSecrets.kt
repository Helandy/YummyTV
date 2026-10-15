package su.afk.yummy.tv.buildlogic

import org.gradle.api.Project
import java.security.MessageDigest
import java.util.Properties

/**
 * Секрет из `local.properties` для `buildConfigField("String", ...)`, уже в кавычках.
 * Не найден — пустая строка: сборка проходит, соответствующий SDK не инициализируется.
 */
fun Project.buildConfigSecret(property: String): String {
    val value = rootProject.localProperties().getProperty(property).orEmpty()
    return value.toBuildConfigString()
}

/**
 * Секрет для `buildConfigField("String", ...)` в виде Java-выражения: байты, замаскированные
 * XOR-ключом, и вызов [decoderFqn] `.decode(byte[], byte[])`. Открытой строки в константах класса
 * нет, поэтому `strings`/grep по APK её не находят. Ключ выводится из имени свойства, чтобы сборка
 * оставалась воспроизводимой. Это не шифрование, а барьер от поверхностного поиска.
 */
fun Project.buildConfigObfuscatedSecret(property: String, decoderFqn: String): String {
    val value = rootProject.localProperties().getProperty(property).orEmpty()
    if (value.isEmpty()) return "\"\""
    val key = MessageDigest.getInstance("SHA-256").digest("yummytv:$property".toByteArray())
    val masked = value.toByteArray().mapIndexed { i, b -> (b.toInt() xor key[i % key.size].toInt()).toByte() }
    return "$decoderFqn.decode(new byte[]{${masked.joinToString(",")}}, new byte[]{${key.joinToString(",")}})"
}

private fun Project.localProperties(): Properties = Properties().apply {
    file("local.properties").takeIf { it.isFile }?.reader()?.use(::load)
}

private fun String.toBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
