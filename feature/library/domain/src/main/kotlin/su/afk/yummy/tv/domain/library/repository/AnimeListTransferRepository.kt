package su.afk.yummy.tv.domain.library.repository

import su.afk.yummy.tv.domain.library.model.ExportableLibraryAnime
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.model.ResolvedExternalAnime

/** Контракт чтения/записи внешних файлов списка аниме и пакетного сопоставления ID с YummyAnime. */
interface AnimeListTransferRepository {

    suspend fun currentUserId(): Int

    suspend fun currentNickname(): String

    /** Читает и разбирает файл списка (`.xml`, `.xml.gz` или `.json`) по `content://` URI. */
    suspend fun parseExternalListUri(uriString: String): ParsedExternalList

    /** Сериализует записи библиотеки в выбранный формат [format]. */
    fun formatExternalList(
        format: ExternalListFormat,
        userName: String,
        entries: List<ExternalListEntry>,
    ): String

    /** Записывает текст экспорта в выбранный пользователем документ SAF (`content://`). */
    suspend fun writeExportToUri(uriString: String, content: String)

    /** Сохраняет экспорт в папку `Downloads` (или внешнюю папку приложения) и возвращает путь. */
    suspend fun saveExportToStorage(
        fileName: String,
        mimeType: String,
        content: String,
    ): String

    /** Создаёт временный файл экспорта и возвращает `content://` URI для системного «Поделиться». */
    suspend fun createShareUri(fileName: String, content: String): String

    /**
     * Загружает библиотеку со связанными ID Shikimori и MyAnimeList: для авторизованного
     * пользователя — одним запросом `GET /users/{id}/lists`, для гостя — локальная таблица
     * `library` плюс пакетное дообогащение через `GET /anime?ids=...`.
     */
    suspend fun loadExportableLibrary(): List<ExportableLibraryAnime>

    /** Возвращает количество уникальных досмотренных серий по `animeId` из локального хранилища. */
    suspend fun loadLocalWatchedEpisodeCounts(): Map<Int, Int>

    /**
     * Помечает серии `1..watchedEpisodes` просмотренными в локальной таблице `watch_progress`,
     * не затирая уже существующие записи и не обращаясь к серверному `/video`.
     */
    suspend fun markEpisodesWatchedLocally(
        animeId: Int,
        animeTitle: String,
        posterUrl: String,
        watchedEpisodes: Int,
    )

    /**
     * Пакетно сопоставляет внешние ID с каталогом YummyAnime (`GET /anime?shikimori_ids=...` или
     * `mal_ids=...`) с вызовом [onBatchProgress] после каждого обработанного чанка.
     */
    suspend fun resolveByExternalIds(
        format: ExternalListFormat,
        externalIds: List<Int>,
        onBatchProgress: suspend (processedIds: Int, totalIds: Int) -> Unit = { _, _ -> },
    ): Map<Int, List<ResolvedExternalAnime>>
}
