package su.afk.yummy.tv.domain.library.model

/** Результат предварительного разбора файла импорта перед записью в библиотеку. */
data class ParsedExternalList(
    val format: ExternalListFormat,
    val entries: List<ExternalListEntry>,
    /** Записи, отфильтрованные по типу (например, манга/ранобэ в JSON Shikimori). */
    val skippedNonAnimeCount: Int = 0,
    /** Записи без корректного ID или с нераспознанным статусом. */
    val skippedInvalidCount: Int = 0,
)
