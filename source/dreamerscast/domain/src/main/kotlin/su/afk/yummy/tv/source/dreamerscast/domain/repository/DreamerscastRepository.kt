package su.afk.yummy.tv.source.dreamerscast.domain.repository

import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastStreams
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget

interface DreamerscastRepository {
    /** Потоки серии; `null`, если страницы, плеера, серии или HLS-ссылки на сайте нет. Сбой сети уходит исключением. */
    suspend fun getStreams(target: DreamerscastTarget): DreamerscastStreams?
}
