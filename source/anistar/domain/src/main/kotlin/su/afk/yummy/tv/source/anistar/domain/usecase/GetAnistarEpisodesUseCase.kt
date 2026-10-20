package su.afk.yummy.tv.source.anistar.domain.usecase

import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import javax.inject.Inject

/** Отдаёт серии плейлиста тайтла на AniStar по возрастанию номера; серии с одним номером схлопываются в первую. */
class GetAnistarEpisodesUseCase @Inject constructor(
    private val repository: AnistarRepository,
) {
    suspend operator fun invoke(title: AnistarTitle): List<AnistarEpisode> =
        repository.getEpisodes(title).distinctBy { it.number }.sortedBy { it.number }
}
