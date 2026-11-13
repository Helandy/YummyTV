package su.afk.yummy.tv.core.model.settings

/**
 * Блок экрана деталей под шапкой. Порядок значений — порядок по умолчанию.
 * На ТВ [INFO] и [DESCRIPTION] живут в шапке: там их можно только скрыть, порядок не действует.
 */
enum class DetailsSectionKind {
    /** Студии, создатели и жанры. */
    INFO,
    DESCRIPTION,
    SCREENSHOTS,
    VIDEO,
    RATING,
    UNIVERSE,
    SIMILAR,
    COLLECTIONS,
    REVIEWS,
    COMMENTS,
}
