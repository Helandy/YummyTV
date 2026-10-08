package su.afk.yummy.tv.data.player.extractor.cvh

/** Тег плейлиста CdnVideoHub «Blocked»: контент запрещён на территории РФ (enum в JS плеера). */
internal const val CVH_TAG_BLOCKED = 5

/**
 * Плейлист закрыт запретом на территории РФ: источник помечает его тегом [CVH_TAG_BLOCKED] и
 * отдаёт пустой список. Тег при непустом списке запретом не считаем — серии ещё можно играть.
 */
internal fun isCvhRegionBlocked(tags: List<Int>, hasItems: Boolean): Boolean =
    !hasItems && CVH_TAG_BLOCKED in tags
