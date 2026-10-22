package su.afk.yummy.tv.source.aniboom.domain.repository

import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreams
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTitle

interface AniboomRepository {
    /** Спрашивает у сервера, есть ли тайтл в AniBoom; `null` — нет. Сбой сети уходит исключением. */
    suspend fun findTitle(lookup: AniboomLookup): AniboomTitle?

    /** Поток серии из эмбеда AniBoom; `null`, если такой серии или озвучки там нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AniboomTarget): AniboomStreams?
}
