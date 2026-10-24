package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

/** Обновляет локальные метаданные тайтла в библиотеке. */
class RefreshLibraryMetadataUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(
        animeId: Int,
        title: String,
        poster: AnimePoster?,
        year: Int?,
    ) = repository.refreshMetadata(animeId, title, poster, year)
}
