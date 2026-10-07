package su.afk.yummy.tv.core.preferences.settings.datastore

import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.core.preferences.settings.SettingsStore

internal const val LIBRARY_TAB_ORDER_SEPARATOR = "|"

/** Разбирает сохранённый порядок вкладок библиотеки, дополняя его недостающими вкладками. */
internal fun String?.toLibraryTabOrder(): List<LibraryTabKind> {
    if (isNullOrBlank()) return SettingsStore.defaultLibraryTabOrder
    return split(LIBRARY_TAB_ORDER_SEPARATOR)
        .mapNotNull { name -> runCatching { LibraryTabKind.valueOf(name) }.getOrNull() }
        .normalizedLibraryTabOrder()
}

/**
 * Убирает дубли, дополняет порядок отсутствующими вкладками и держит закреплённую вкладку
 * ([LibraryTabKind.isPinnedFirst]) на первой позиции.
 */
internal fun List<LibraryTabKind>.normalizedLibraryTabOrder(): List<LibraryTabKind> {
    val unique = distinct()
    val complete = unique + SettingsStore.defaultLibraryTabOrder.filterNot { it in unique }
    return complete.filter { it.isPinnedFirst } + complete.filterNot { it.isPinnedFirst }
}
