package su.afk.yummy.tv.feature.settings.utils

import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

/**
 * Меняет вкладку местами с соседней в указанном направлении; на краю списка порядок не меняется.
 * Закреплённая вкладка не двигается, и другие вкладки не могут занять её место.
 */
internal fun List<LibraryTabKind>.movedLibraryTab(
    tab: LibraryTabKind,
    direction: DetailsButtonMoveDirection,
): List<LibraryTabKind> {
    if (tab.isPinnedFirst) return this
    val index = indexOf(tab)
    if (index == -1) return this
    val targetIndex = when (direction) {
        DetailsButtonMoveDirection.UP -> index - 1
        DetailsButtonMoveDirection.DOWN -> index + 1
    }
    if (targetIndex !in indices || this[targetIndex].isPinnedFirst) return this
    return toMutableList().apply {
        this[index] = this[targetIndex]
        this[targetIndex] = tab
    }
}
