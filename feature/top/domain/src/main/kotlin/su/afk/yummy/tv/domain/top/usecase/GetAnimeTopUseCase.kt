package su.afk.yummy.tv.domain.top.usecase

import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.repository.AnimeTopRepository

/** Loads a paged top anime list for the selected ranking type. */
class GetAnimeTopUseCase(
    private val repository: AnimeTopRepository,
) {
    suspend operator fun invoke(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        repository.getTopAnime(type, limit, offset)
}
