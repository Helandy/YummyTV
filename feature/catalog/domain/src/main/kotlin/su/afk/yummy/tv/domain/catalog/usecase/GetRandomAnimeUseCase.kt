package su.afk.yummy.tv.domain.catalog.usecase

import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.repository.SearchRepository
import javax.inject.Inject

/** Returns one random anime from the full catalog. */
class GetRandomAnimeUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(): SearchItem? = repository.getRandomAnime()
}
