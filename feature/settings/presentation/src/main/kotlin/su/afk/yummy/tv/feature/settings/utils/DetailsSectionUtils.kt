package su.afk.yummy.tv.feature.settings.utils

import su.afk.yummy.tv.core.model.settings.DetailsSectionConfig
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

/** Включает выключенный блок деталей и наоборот; порядок не меняется. */
internal fun List<DetailsSectionConfig>.toggledDetailsSection(section: DetailsSectionKind): List<DetailsSectionConfig> =
    map { if (it.kind == section) it.copy(enabled = !it.enabled) else it }

/** Меняет блок деталей местами с соседним в указанном направлении; на краю списка порядок не меняется. */
internal fun List<DetailsSectionConfig>.movedDetailsSection(
    section: DetailsSectionKind,
    direction: DetailsButtonMoveDirection,
): List<DetailsSectionConfig> = swappedWithNeighbor(indexOfFirst { it.kind == section }, direction)
