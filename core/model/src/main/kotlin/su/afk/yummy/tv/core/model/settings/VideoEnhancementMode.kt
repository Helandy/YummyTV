package su.afk.yummy.tv.core.model.settings

/** Улучшение картинки в плеере; одновременно активен только один режим. */
enum class VideoEnhancementMode {
    /** Без обработки: видео идёт прямо на аппаратный вывод, режим картинки ТВ работает. */
    OFF,

    /** Лёгкая резкость контуров: Anime4K Deblur DoG, без ореолов. */
    SHARPEN,

    /** Anime4K Restore CNN S: восстанавливает линии и убирает артефакты сжатия; тяжелее резкости. */
    QUALITY,
}
