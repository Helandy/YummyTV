package su.afk.yummy.tv.domain.library.model

/** Подготовленный к сохранению или отправке файл экспорта библиотеки пользователя. */
data class AnimeListExportResult(
    val format: ExternalListFormat,
    val content: String,
    val suggestedFileName: String,
    val exportedCount: Int,
    /** Тайтлы, пропущенные из-за отсутствия привязки `shikimori_id` / `myanimelist_id` на YummyAnime. */
    val skippedWithoutExternalIdCount: Int,
    /** Тайтлы, лежащие только в «Любимых» без статуса списка (во внешних форматах такого статуса нет). */
    val skippedFavoriteOnlyCount: Int,
)
