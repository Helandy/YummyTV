package su.afk.yummy.tv.domain.top100.repository

import su.afk.yummy.tv.domain.top100.model.AnimeTopPage
import su.afk.yummy.tv.domain.top100.model.AnimeTopType

interface AnimeTopRepository {
    suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage
}
