package su.afk.yummy.tv.source.anime4k.domain.repository

import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kLookup
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTitle

interface Anime4kRepository {
    /**
     * Серии и озвучки с 4K без ссылок. `null`, если у тайтла 4K нет. Сервер ищет сначала по MAL id,
     * у тайтлов без него — по названиям из [lookup].
     */
    suspend fun getTitle(lookup: Anime4kLookup): Anime4kTitle?

    /** Ссылки на файлы серии и озвучки из [target]. Сервер отдаёт их только подписанному запросу приложения. */
    suspend fun getStreams(target: Anime4kTarget): Anime4kStreams
}
