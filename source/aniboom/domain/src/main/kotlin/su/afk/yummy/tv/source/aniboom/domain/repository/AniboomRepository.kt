package su.afk.yummy.tv.source.aniboom.domain.repository

import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreams
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget

interface AniboomRepository {
    /** Поток серии из эмбеда AniBoom; `null`, если такой серии или озвучки там нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AniboomTarget): AniboomStreams?
}
