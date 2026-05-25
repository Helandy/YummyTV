package su.afk.yummy.tv.domain.anime.repository

import su.afk.yummy.tv.domain.anime.model.AnimePreview

interface AnimePreviewRepository {
    suspend fun getAnimePreview(animeId: Int): AnimePreview
}
