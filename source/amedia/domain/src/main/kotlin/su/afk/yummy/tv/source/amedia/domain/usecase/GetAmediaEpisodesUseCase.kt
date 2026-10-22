package su.afk.yummy.tv.source.amedia.domain.usecase

import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import javax.inject.Inject

/** Отдаёт вышедшие серии Amedia-плеера по возрастанию номера; серии с одним номером схлопываются в первую. */
class GetAmediaEpisodesUseCase @Inject constructor(
    private val repository: AmediaRepository,
) {
    suspend operator fun invoke(title: AmediaTitle): List<AmediaEpisode> =
        repository.getEpisodes(title).distinctBy { it.number }.sortedBy { it.number }
}
