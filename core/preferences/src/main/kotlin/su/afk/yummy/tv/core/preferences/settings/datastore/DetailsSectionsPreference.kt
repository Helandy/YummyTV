package su.afk.yummy.tv.core.preferences.settings.datastore

import su.afk.yummy.tv.core.model.settings.DetailsSectionConfig
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind

private const val DETAILS_SECTIONS_SEPARATOR = "|"
private const val DETAILS_SECTION_DISABLED_SUFFIX = ":0"

/** Разбирает сохранённые блоки деталей (`KIND` или `KIND:0` для выключенного, через `|`). */
internal fun String?.toDetailsSections(): List<DetailsSectionConfig> {
    if (isNullOrBlank()) return DetailsSectionConfig.default
    return split(DETAILS_SECTIONS_SEPARATOR)
        .mapNotNull { token ->
            val name = token.removeSuffix(DETAILS_SECTION_DISABLED_SUFFIX)
            val kind = runCatching { DetailsSectionKind.valueOf(name) }.getOrNull() ?: return@mapNotNull null
            DetailsSectionConfig(kind, enabled = !token.endsWith(DETAILS_SECTION_DISABLED_SUFFIX))
        }
        .normalizedDetailsSections()
}

/**
 * Убирает дубли и добавляет отсутствующие блоки (новые версии приложения) на их место по умолчанию:
 * сразу после ближайшего предшествующего им блока из стандартного порядка.
 */
internal fun List<DetailsSectionConfig>.normalizedDetailsSections(): List<DetailsSectionConfig> {
    val result = distinctBy { it.kind }.toMutableList()
    val defaults = DetailsSectionConfig.default
    defaults.forEachIndexed { defaultIndex, missing ->
        if (result.any { it.kind == missing.kind }) return@forEachIndexed
        val anchor = defaults.subList(0, defaultIndex).lastOrNull { prev -> result.any { it.kind == prev.kind } }
        val insertAt = anchor?.let { prev -> result.indexOfFirst { it.kind == prev.kind } + 1 } ?: 0
        result.add(insertAt, missing)
    }
    return result
}

internal fun List<DetailsSectionConfig>.toDetailsSectionsPreference(): String =
    normalizedDetailsSections().joinToString(DETAILS_SECTIONS_SEPARATOR) {
        if (it.enabled) it.kind.name else it.kind.name + DETAILS_SECTION_DISABLED_SUFFIX
    }
