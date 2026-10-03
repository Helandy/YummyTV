package su.afk.yummy.tv.core.model.settings

/** Поле, по которому упорядочены списки библиотеки. */
enum class LibrarySort {
    /** Дата добавления в список (в избранном — дата добавления в избранное). */
    ADDED_DATE,
    YEAR,

    /** Общий рейтинг тайтла. */
    RATING,

    /** Оценка пользователя. */
    USER_RATING,

    /** Название тайтла по алфавиту. */
    TITLE,
}

enum class LibrarySortDirection {
    DESC,
    ASC,
    ;

    /** Противоположное направление сортировки. */
    fun toggled(): LibrarySortDirection = when (this) {
        DESC -> ASC
        ASC -> DESC
    }
}
