package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.model.AnimeListExportResult
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import javax.inject.Inject

/** Сохраняет подготовленный экспорт списка аниме по выбранному URI или в папку `Downloads`. */
class SaveExportedAnimeListUseCase @Inject constructor(
    private val repository: AnimeListTransferRepository,
) {
    suspend fun saveToUri(uriString: String, export: AnimeListExportResult) {
        repository.writeExportToUri(uriString = uriString, content = export.content)
    }

    suspend fun saveToStorage(export: AnimeListExportResult): String =
        repository.saveExportToStorage(
            fileName = export.suggestedFileName,
            mimeType = export.format.mimeType,
            content = export.content,
        )
}
