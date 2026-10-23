package su.afk.yummy.tv.source.amedia.domain.repository

import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle

interface AmediaRepository {
    /** Спрашивает у сервера, есть ли тайтл на amd.online, и берёт у него же вышедшие серии; `null` — тайтла нет. Сбой сети уходит исключением. */
    suspend fun findTitle(lookup: AmediaLookup): AmediaTitle?

    /** Свежие ссылки на HLS серии; `null`, если плеер не отдал поток. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AmediaTarget): AmediaStreams?
}
