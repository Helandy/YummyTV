package su.afk.yummy.tv.core.utils.formatting

/**
 * Упрощает markdown из описания GitHub-релиза до обычного текста: убирает `#` заголовков,
 * выделение и код, ссылки заменяет их текстом, `* ` в списках — на `- `.
 */
fun String.formatReleaseNotes(): String =
    lines()
        .joinToString(separator = "\n") { line ->
            val trimmedEnd = line.trimEnd()
            val content = trimmedEnd.trimStart()
            val indent = trimmedEnd.take(trimmedEnd.length - content.length)
            when {
                content.startsWith("#") -> content.replace(Regex("^#{1,6}\\s*"), "")
                content.startsWith("* ") -> indent + "- " + content.removePrefix("* ")
                else -> trimmedEnd
            }
        }
        .replace(Regex("""\[(.*?)]\((.*?)\)"""), "$1")
        .replace("**", "")
        .replace("*", "")
        .replace("`", "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
