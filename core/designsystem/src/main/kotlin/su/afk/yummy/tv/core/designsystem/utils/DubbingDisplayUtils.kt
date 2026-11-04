package su.afk.yummy.tv.core.designsystem.utils

/** Как озвучка называется у yani (ключ без служебной приставки, регистра и знаков) → как её показываем пользователю. */
private val DISPLAY_NAMES = mapOf(
    "творческаястудиямир" to "Студия Мир",
    "студиямир" to "Студия Мир",
    "studiomir" to "Студия Мир",
)

/**
 * Имя озвучки для показа. Приставка типа в начале («Озвучка », «Субтитры », «Дубляж »…) сохраняется как написана.
 * Только отображение: идентификатором (прогресс, подписки, звёзды, сопоставление источников)
 * остаётся исходное имя, поэтому в логику это значение не передавать.
 */
fun String.dubbingDisplayName(): String {
    val words = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    var prefixCount = 0
    while (prefixCount < words.size - 1 && words[prefixCount].lowercase() in LOGO_LABEL_PREFIXES) prefixCount++
    val rest = words.drop(prefixCount).joinToString(" ")
    val display = DISPLAY_NAMES[rest.logoKey()] ?: return this
    return (words.take(prefixCount) + display).joinToString(" ")
}
