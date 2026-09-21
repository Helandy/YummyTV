package su.afk.yummy.tv.domain.update.util

/** Суффикс версии бета-сборки: `versionName` беты — `1.21.1.2-beta` (2-я бета после стабильной `1.21.1`). */
const val PRERELEASE_VERSION_SUFFIX = "-beta"

/**
 * Сравнивает версии вида `1.21.1` и `1.21.1.2-beta`: сначала числовые части любой длины
 * (недостающие = 0), при равенстве стабильная версия старше pre-release (любой суффикс после `-`).
 * Порядок: `1.21.1` < `1.21.1.2-beta` < `1.21.1.10-beta` < `1.21.2`.
 */
fun compareVersions(a: String, b: String): Int {
    val left = ParsedVersion.parse(a)
    val right = ParsedVersion.parse(b)
    val len = maxOf(left.parts.size, right.parts.size)
    for (i in 0 until len) {
        val diff = left.parts.getOrElse(i) { 0 }.compareTo(right.parts.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return right.isPrerelease.compareTo(left.isPrerelease)
}

fun isVersionNewer(current: String, remote: String): Boolean = compareVersions(remote, current) > 0

private class ParsedVersion(val parts: List<Int>, val isPrerelease: Boolean) {
    companion object {
        fun parse(version: String): ParsedVersion {
            val trimmed = version.trim()
            val numeric = trimmed.substringBefore('-')
            return ParsedVersion(
                parts = numeric.split(".").map { it.toIntOrNull() ?: 0 },
                isPrerelease = numeric.length != trimmed.length,
            )
        }
    }
}
