package su.afk.yummy.tv.source.anime4k.domain.repository

import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kLookup
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTicket
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTitle

interface Anime4kRepository {
    /**
     * Серии и озвучки с 4K без ссылок. `null`, если у тайтла 4K нет. Реклама не нужна. Сервер ищет сначала по MAL id,
     * у тайтлов без него — по названиям из [lookup].
     */
    suspend fun getTitle(lookup: Anime4kLookup): Anime4kTitle?

    /** Тикет на выдачу ссылок. Выдаётся до рекламы: сервер считает от него минимальное ожидание. */
    suspend fun requestTicket(): Anime4kTicket

    /** Ссылки на файлы по погашенному тикету. */
    suspend fun getStreams(ticket: Anime4kTicket, target: Anime4kTarget): Anime4kStreams
}
