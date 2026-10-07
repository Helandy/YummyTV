package su.afk.yummy.tv.feature.library.model

enum class LibraryTab {
    HISTORY,
    CONTINUE_WATCHING,
    FAVORITES,
    WATCHING,
    PLANNED,
    COMPLETED,
    POSTPONED,
    DROPPED,

    ;

    /** Сортировка доступна только на вкладках-списках: у «Продолжить» и «Истории» свой порядок. */
    val hasSort: Boolean get() = this != CONTINUE_WATCHING && this != HISTORY
}
