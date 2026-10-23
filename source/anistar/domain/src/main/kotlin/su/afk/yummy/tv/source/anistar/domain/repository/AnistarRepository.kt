package su.afk.yummy.tv.source.anistar.domain.repository

import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle

interface AnistarRepository {
    /** Спрашивает у сервера, есть ли тайтл на AniStar, и берёт у него же номера серий; `null` — нет. Сбой сети уходит исключением. */
    suspend fun findTitle(lookup: AnistarLookup): AnistarTitle?

    /**
     * Свежие ссылки на HLS серии (ссылки с токеном, из кэша не берутся); `null`, если такой серии в плейлисте нет.
     * Сбой сети уходит исключением.
     */
    suspend fun getStreams(target: AnistarTarget): AnistarStreams?
}
