package su.afk.yummy.tv.core.utils.episode

fun String.episodeNumberOrNull(): Double? {
    val normalized = trim().replace(',', '.')
    return normalized.toDoubleOrNull()
        ?: EPISODE_NUMBER_REGEX
            .find(normalized)
            ?.value
            ?.replace(',', '.')
            ?.toDoubleOrNull()
}

fun String.isPlaceholderEpisode(): Boolean = trim().isEmpty() || trim() == "-"

/**
 * Нормализованный ключ серии для группировки/дедупа.
 * Разные озвучки присылают номер по-разному (`"01"` vs `"1"`), схлопываем их в один ключ.
 */
fun String.episodeGroupKey(): String =
    trim().trimStart('0').ifEmpty { trim() }.lowercase()

/**
 * Каноничный номер серии для хранения и показа: yani присылает одну и ту же серию
 * и как `"02"`, и как `"1"`, из-за чего точные сравнения строк разваливают выбор
 * озвучки/балансера. Ведущие нули срезаем только у чисто числовых номеров, чтобы
 * не портить `"OVA 1"` и подобные. Согласован с [episodeGroupKey].
 */
fun String.normalizedEpisodeNumber(): String {
    val trimmed = trim()
    if (trimmed.isEmpty() || !trimmed.all { it.isDigit() }) return trimmed
    return trimmed.trimStart('0').ifEmpty { "0" }
}

private val EPISODE_NUMBER_REGEX = Regex("""\d+(?:[.,]\d+)?""")
