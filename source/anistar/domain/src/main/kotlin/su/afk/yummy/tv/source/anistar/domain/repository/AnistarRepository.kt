package su.afk.yummy.tv.source.anistar.domain.repository

import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget

interface AnistarRepository {
    /**
     * Свежие ссылки на HLS серии (ссылки с токеном, из кэша не берутся); `null`, если такой серии в плейлисте нет.
     * Сбой сети уходит исключением.
     */
    suspend fun getStreams(target: AnistarTarget): AnistarStreams?
}
