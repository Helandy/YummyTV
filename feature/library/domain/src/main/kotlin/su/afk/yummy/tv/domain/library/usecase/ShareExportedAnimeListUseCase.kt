package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.model.AnimeListExportResult
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import javax.inject.Inject

/** Создаёт временный файл экспорта и возвращает `content://` URI для системного «Поделиться». */
class ShareExportedAnimeListUseCase @Inject constructor(
    private val repository: AnimeListTransferRepository,
) {
    suspend operator fun invoke(export: AnimeListExportResult): String =
        repository.createShareUri(
            fileName = export.suggestedFileName,
            content = export.content,
        )
}
