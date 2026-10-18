package su.afk.yummy.tv.core.model.settings

/**
 * Вкладка библиотеки в том виде, в каком её порядок хранится в настройках. Порядок значений — порядок
 * по умолчанию: списки как в профиле, «Любимые» в конце.
 */
enum class LibraryTabKind {
    HISTORY,
    CONTINUE_WATCHING,
    WATCHING,
    PLANNED,
    COMPLETED,
    POSTPONED,
    DROPPED,
    FAVORITES,
    ;

    /** Вкладка закреплена на первой позиции: её нельзя сдвинуть, и другие вкладки не встают выше. */
    val isPinnedFirst: Boolean get() = this == HISTORY
}
