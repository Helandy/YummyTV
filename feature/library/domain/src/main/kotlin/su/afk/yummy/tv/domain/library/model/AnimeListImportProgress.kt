package su.afk.yummy.tv.domain.library.model

/** Текущий прогресс выполнения импорта списка аниме. */
data class AnimeListImportProgress(
    val phase: AnimeListImportPhase,
    val processed: Int,
    val total: Int,
)
