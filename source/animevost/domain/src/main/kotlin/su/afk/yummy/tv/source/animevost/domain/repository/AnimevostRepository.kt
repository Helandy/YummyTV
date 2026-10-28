package su.afk.yummy.tv.source.animevost.domain.repository

import su.afk.yummy.tv.source.animevost.domain.model.AnimevostStreams
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget

interface AnimevostRepository {
    /** Потоки серии; `null`, если страницы, серии или ссылок на сайте нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AnimevostTarget): AnimevostStreams?
}
