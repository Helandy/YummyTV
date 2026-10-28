package su.afk.yummy.tv.source.ailiberty.domain.repository

import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyStreams
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyTarget

interface AiLibertyRepository {
    /** Потоки серии со страницы релиза; `null`, если релиза или серии на сайте нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: AiLibertyTarget): AiLibertyStreams?
}
