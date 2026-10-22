package su.afk.yummy.tv.source.amedia.domain.repository

import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle

interface AmediaRepository {
    /** Ищет тайтл на amd.online и сверяет MAL id; `null` — такой страницы нет. Сбой сети уходит исключением. */
    suspend fun findTitle(lookup: AmediaLookup): AmediaTitle?

    /** Вышедшие серии Amedia-плеера по возрастанию номера; пусто, если серий нет. Сбой сети уходит исключением. */
    suspend fun getEpisodes(title: AmediaTitle): List<AmediaEpisode>

    /** Свежие ссылки на HLS серии; `null`, если плеер не отдал поток. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AmediaTarget): AmediaStreams?
}
