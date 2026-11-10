package su.afk.yummy.tv.core.preferences.settings.datastore

import su.afk.yummy.tv.core.model.settings.HomeBlockConfig
import su.afk.yummy.tv.core.model.settings.HomeBlockKind

private const val HOME_BLOCKS_SEPARATOR = "|"
private const val HOME_BLOCK_DISABLED_SUFFIX = ":0"

/**
 * Разбирает сохранённые блоки главной (`KIND` или `KIND:0` для выключенного, через `|`).
 * Если настройка ещё не сохранялась, берёт значения по умолчанию, а блок «новые серии» —
 * по прежнему отдельному переключателю [legacyNewEpisodesEnabled].
 */
internal fun String?.toHomeBlocks(legacyNewEpisodesEnabled: Boolean = true): List<HomeBlockConfig> {
    if (isNullOrBlank()) {
        return HomeBlockConfig.default.map {
            if (it.kind == HomeBlockKind.MY_NEW_EPISODES) it.copy(enabled = legacyNewEpisodesEnabled) else it
        }
    }
    return split(HOME_BLOCKS_SEPARATOR)
        .mapNotNull { token ->
            val name = token.removeSuffix(HOME_BLOCK_DISABLED_SUFFIX)
            val kind = runCatching { HomeBlockKind.valueOf(name) }.getOrNull() ?: return@mapNotNull null
            HomeBlockConfig(kind, enabled = !token.endsWith(HOME_BLOCK_DISABLED_SUFFIX))
        }
        .normalizedHomeBlocks()
}

/** Убирает дубли и дописывает отсутствующие блоки (новые версии приложения) в конец. */
internal fun List<HomeBlockConfig>.normalizedHomeBlocks(): List<HomeBlockConfig> {
    val unique = distinctBy { it.kind }
    val known = unique.map { it.kind }.toSet()
    return unique + HomeBlockConfig.default.filterNot { it.kind in known }
}

internal fun List<HomeBlockConfig>.toHomeBlocksPreference(): String =
    normalizedHomeBlocks().joinToString(HOME_BLOCKS_SEPARATOR) {
        if (it.enabled) it.kind.name else it.kind.name + HOME_BLOCK_DISABLED_SUFFIX
    }
