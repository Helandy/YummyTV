package su.afk.yummy.tv.domain.library.model

/** Этап выполнения импорта списка аниме. */
enum class AnimeListImportPhase {
    /** Пакетное сопоставление ID Shikimori / MAL с каталогом YummyAnime. */
    RESOLVING_TITLES,

    /** Чтение текущего состояния библиотеки пользователя для вычисления дельты. */
    LOADING_CURRENT_LIST,

    /** Последовательное сохранение новых и изменённых записей. */
    APPLYING_CHANGES,
}
