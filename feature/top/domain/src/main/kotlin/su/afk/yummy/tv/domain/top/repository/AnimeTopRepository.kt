package su.afk.yummy.tv.domain.top.repository

import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType

interface AnimeTopRepository {
    suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage
}
