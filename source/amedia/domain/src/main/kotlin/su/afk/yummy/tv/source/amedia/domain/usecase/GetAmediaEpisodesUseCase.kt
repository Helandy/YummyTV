package su.afk.yummy.tv.source.amedia.domain.usecase

import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import javax.inject.Inject

/** Отдаёт вышедшие серии Amedia-плеера из ответа сервера по возрастанию номера; серии с одним номером схлопываются в первую. */
class GetAmediaEpisodesUseCase @Inject constructor() {
    operator fun invoke(title: AmediaTitle): List<AmediaEpisode> =
        title.episodes.distinctBy { it.number }.sortedBy { it.number }
}
