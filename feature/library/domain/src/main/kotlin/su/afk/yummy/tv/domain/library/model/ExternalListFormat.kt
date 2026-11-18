package su.afk.yummy.tv.domain.library.model

/** Формат файла внешнего списка аниме для импорта и экспорта. */
enum class ExternalListFormat(
    val fileExtension: String,
    val mimeType: String,
) {
    /** XML-формат MyAnimeList; поддерживается импортом и на MyAnimeList, и на Shikimori. */
    MAL_XML(fileExtension = "xml", mimeType = "application/xml"),

    /** JSON-формат экспорта пользовательского списка Shikimori (`*_animes.json`). */
    SHIKIMORI_JSON(fileExtension = "json", mimeType = "application/json"),
}
