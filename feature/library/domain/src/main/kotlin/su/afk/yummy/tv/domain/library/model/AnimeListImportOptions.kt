package su.afk.yummy.tv.domain.library.model

/** Параметры применения импортируемого списка к библиотеке пользователя. */
data class AnimeListImportOptions(
    val conflictPolicy: ImportConflictPolicy = ImportConflictPolicy.SKIP_EXISTING,
    /** Переносить ли пользовательские оценки `1..10`. */
    val importScores: Boolean = true,
    /** Помечать ли просмотренные серии в локальном хранилище устройства (без запросов к `/video`). */
    val importWatchedEpisodesLocally: Boolean = true,
)
