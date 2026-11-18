package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import javax.inject.Inject

/** Разбирает файл списка аниме (Shikimori JSON или MyAnimeList XML / XML.GZ) для предпросмотра и импорта. */
class ParseExternalAnimeListUseCase @Inject constructor(
    private val repository: AnimeListTransferRepository,
) {
    suspend operator fun invoke(uriString: String): ParsedExternalList =
        repository.parseExternalListUri(uriString)
}
