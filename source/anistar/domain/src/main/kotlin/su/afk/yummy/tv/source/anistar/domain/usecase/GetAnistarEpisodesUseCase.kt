package su.afk.yummy.tv.source.anistar.domain.usecase

import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import javax.inject.Inject

/** Отдаёт серии тайтла на AniStar из ответа сервера по возрастанию номера; серии с одним номером схлопываются в первую. */
class GetAnistarEpisodesUseCase @Inject constructor() {
    operator fun invoke(title: AnistarTitle): List<AnistarEpisode> =
        title.episodes.distinctBy { it.number }.sortedBy { it.number }
}
