package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import javax.inject.Inject

class RefreshLibraryMetadataUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(
        animeId: Int,
        title: String,
        poster: LibraryPoster?,
    ) = repository.refreshMetadata(animeId, title, poster)
}
