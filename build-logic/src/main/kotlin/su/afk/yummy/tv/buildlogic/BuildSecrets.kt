package su.afk.yummy.tv.buildlogic

import org.gradle.api.Project
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
 * Несекретная настройка из `local.properties` для `buildConfigField("String", ...)` (например, домен своего сервера):
 * не задана — [default], чтобы в git лежал только заглушечный адрес.
 */
fun Project.buildConfigProperty(property: String, default: String): String {
    val value = rootProject.localProperties().getProperty(property).orEmpty().trim()
    return (value.ifEmpty { default }).toBuildConfigString()
}

private fun Project.localProperties(): Properties = Properties().apply {
    file("local.properties").takeIf { it.isFile }?.reader()?.use(::load)
}

private fun String.toBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
