package su.afk.yummy.tv.feature.settings.utils

import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

/** Меняет элемент [index] местами с соседним в указанном направлении; на краю списка порядок не меняется. */
internal fun <T> List<T>.swappedWithNeighbor(index: Int, direction: DetailsButtonMoveDirection): List<T> {
    if (index !in indices) return this
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
