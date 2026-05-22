package su.afk.yummy.tv.domain.anime

interface AnimePreviewRepository {
    suspend fun getAnimePreview(animeId: Int): AnimePreview
}
