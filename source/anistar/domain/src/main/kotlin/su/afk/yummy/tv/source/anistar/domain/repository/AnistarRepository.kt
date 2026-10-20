package su.afk.yummy.tv.source.anistar.domain.repository

import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle

interface AnistarRepository {
    /** Спрашивает у сервера, есть ли тайтл на AniStar; `null` — нет. Сбой сети уходит исключением. */
    suspend fun findTitle(lookup: AnistarLookup): AnistarTitle?

    /** Серии плейлиста тайтла по возрастанию номера; пусто, если плейлист пуст. Сбой сети уходит исключением. */
    suspend fun getEpisodes(title: AnistarTitle): List<AnistarEpisode>

    /**
     * Свежие ссылки на HLS серии (ссылки с токеном, из кэша не берутся); `null`, если такой серии в плейлисте нет.
     * Сбой сети уходит исключением.
     */
    suspend fun getStreams(target: AnistarTarget): AnistarStreams?
}
