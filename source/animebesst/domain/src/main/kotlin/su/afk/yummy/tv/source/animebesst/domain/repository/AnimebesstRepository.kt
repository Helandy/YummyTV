package su.afk.yummy.tv.source.animebesst.domain.repository

import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstStreams
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget

interface AnimebesstRepository {
    /** Потоки серии; `null`, если страницы, серии, плеера или файла на сайте нет. Сбой сети на всех хостах уходит исключением. */
    suspend fun getStreams(target: AnimebesstTarget): AnimebesstStreams?
}
