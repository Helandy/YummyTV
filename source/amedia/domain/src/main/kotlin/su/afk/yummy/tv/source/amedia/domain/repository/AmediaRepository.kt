package su.afk.yummy.tv.source.amedia.domain.repository

import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget

interface AmediaRepository {
    /** Свежие ссылки на HLS серии; `null`, если плеер не отдал поток. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AmediaTarget): AmediaStreams?
}
