package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Загружает краткие данные коллекций, в которых есть выбранное аниме. */
class GetAnimeCollectionsUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int, limit: Int = 20, offset: Int = 0): List<AnimeCollectionSummary> =
        repository.getCollections(animeId, limit, offset)
}
