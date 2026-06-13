package su.afk.yummy.tv.feature.settings.utils

import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.feature.settings.DetailsButtonMoveDirection

internal fun List<DetailsButtonAction>.moved(
    action: DetailsButtonAction,
    direction: DetailsButtonMoveDirection,
): List<DetailsButtonAction> {
    val groups = toDetailsButtonGroups()
    val index = groups.indexOfFirst { action in it }
    if (index == -1) return this
    val targetIndex = when (direction) {
        DetailsButtonMoveDirection.UP -> index - 1
        DetailsButtonMoveDirection.DOWN -> index + 1
    }
    if (targetIndex !in groups.indices) return this
    return groups.toMutableList().apply {
        this[index] = this[targetIndex]
        this[targetIndex] = groups[index]
    }.flatten()
}

private fun List<DetailsButtonAction>.toDetailsButtonGroups(): List<List<DetailsButtonAction>> =
    buildList {
        var index = 0
        while (index <= this@toDetailsButtonGroups.lastIndex) {
            val action = this@toDetailsButtonGroups[index]
            val nextAction = this@toDetailsButtonGroups.getOrNull(index + 1)
            if (action == DetailsButtonAction.LIBRARY && nextAction == DetailsButtonAction.FAVORITE) {
                add(listOf(action, nextAction))
                index += 2
            } else if (action != DetailsButtonAction.FAVORITE) {
                add(listOf(action))
                index += 1
            } else {
                index += 1
            }
        }
    }
