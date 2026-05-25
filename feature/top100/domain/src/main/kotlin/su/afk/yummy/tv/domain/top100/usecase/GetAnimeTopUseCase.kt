package su.afk.yummy.tv.domain.top100.usecase

import su.afk.yummy.tv.domain.top100.model.AnimeTopPage
import su.afk.yummy.tv.domain.top100.model.AnimeTopType
import su.afk.yummy.tv.domain.top100.repository.AnimeTopRepository

/** Loads a paged top anime list for the selected ranking type. */
class GetAnimeTopUseCase(
    private val repository: AnimeTopRepository,
) {
    suspend operator fun invoke(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        repository.getTopAnime(type, limit, offset)
}
