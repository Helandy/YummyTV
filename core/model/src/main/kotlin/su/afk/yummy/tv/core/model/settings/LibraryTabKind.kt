package su.afk.yummy.tv.core.model.settings

/** Вкладка библиотеки в том виде, в каком её порядок хранится в настройках. Порядок значений — порядок по умолчанию. */
enum class LibraryTabKind {
    HISTORY,
    CONTINUE_WATCHING,
    FAVORITES,
    WATCHING,
    PLANNED,
    COMPLETED,
    POSTPONED,
    DROPPED,
    ;

    /** Вкладка закреплена на первой позиции: её нельзя сдвинуть, и другие вкладки не встают выше. */
    val isPinnedFirst: Boolean get() = this == HISTORY
}
