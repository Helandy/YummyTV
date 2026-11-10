package su.afk.yummy.tv.feature.settings.utils

import su.afk.yummy.tv.core.model.settings.HomeBlockConfig
import su.afk.yummy.tv.core.model.settings.HomeBlockKind
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

/** Включает выключенный блок главной и наоборот; порядок не меняется. */
internal fun List<HomeBlockConfig>.toggledHomeBlock(block: HomeBlockKind): List<HomeBlockConfig> =
    map { if (it.kind == block) it.copy(enabled = !it.enabled) else it }

/** Меняет блок местами с соседним в указанном направлении; на краю списка порядок не меняется. */
internal fun List<HomeBlockConfig>.movedHomeBlock(
    block: HomeBlockKind,
    direction: DetailsButtonMoveDirection,
): List<HomeBlockConfig> {
    val index = indexOfFirst { it.kind == block }
    if (index == -1) return this
    val targetIndex = when (direction) {
        DetailsButtonMoveDirection.UP -> index - 1
        DetailsButtonMoveDirection.DOWN -> index + 1
    }
    if (targetIndex !in indices) return this
    return toMutableList().apply {
        val moved = this[index]
        this[index] = this[targetIndex]
        this[targetIndex] = moved
    }
}
