package su.afk.yummy.tv.domain.library.model

/** В библиотеке пользователя нет ни одной записи, пригодной для экспорта во внешний формат. */
class EmptyExportListException(
    val skippedWithoutExternalIdCount: Int = 0,
    val skippedFavoriteOnlyCount: Int = 0,
) : IllegalStateException("No exportable anime list entries")
